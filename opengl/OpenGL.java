package opengl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import custom.Texture;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import vectors.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Vector;

public class OpenGL {
	public static void main(String[] args) throws Exception {
		// let GLFW work on the main thread (for macOS)
		// read the following if you want to create windows with awt/swing/javaFX:
		// https://stackoverflow.com/questions/47006058/lwjgl-java-awt-headlessexception-thrown-when-making-a-jframe
		System.setProperty("java.awt.headless", "true");

		// open a window
		GLFWErrorCallback.createPrint(System.err).set();
		if (!GLFW.glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

		var hWindow = GLFW.glfwCreateWindow(720, 480, "ComGr", 0, 0);
		GLFW.glfwSetWindowSizeCallback(hWindow, (window, width, height) -> {
			var w = new int[1];
			var h = new int[1];
			GLFW.glfwGetFramebufferSize(window, w, h);
			glViewport(0, 0, w[0], h[0]);
		});
		glfwMakeContextCurrent(hWindow);
		GLFW.glfwSwapInterval(1);
		createCapabilities();

		// set up opengl
		if (GLFW.glfwExtensionSupported("GL_KHR_debug")) {
			org.lwjgl.opengl.GL43.glDebugMessageCallback(
					GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
						var msg = GLDebugMessageCallback.getMessage(length, message);
						if (type == org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_ERROR)
							throw new RuntimeException(msg);
						else
							System.out.println(msg);
					}), 0);
			glEnable(org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT);
			glEnable(org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
		}
		glEnable(GL_FRAMEBUFFER_SRGB);
		glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		// glClearDepth(1);
		// glDisable(GL_DEPTH_TEST);
		// glDepthFunc(GL_LESS);
		// glDisable(GL_CULL_FACE);

		// load, compile and link shaders
		// see https://www.khronos.org/opengl/wiki/Vertex_Shader
		var VertexShaderSource = """
				#version 400 core
				
				uniform float inTime;
				uniform mat4 inMatrix;
				out float fromVertexShaderToFragmentShader;
				in vec3 inPos;
				in vec3 inColor;
				out vec3 color;
				
				void main()
				{
					color = inColor;
					gl_Position = inMatrix * vec4(inPos, 1.0) + vec4(sin(inTime) * 0.5, cos(inTime) * 0.5, 0.0, 0.0);
					fromVertexShaderToFragmentShader = inPos.x + 0.5;
				}
				""";
		var hVertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(hVertexShader, VertexShaderSource);
		glCompileShader(hVertexShader);
		if (glGetShaderi(hVertexShader, GL_COMPILE_STATUS) != GL_TRUE)
			throw new Exception(glGetShaderInfoLog(hVertexShader));

		// see https://www.khronos.org/opengl/wiki/Fragment_Shader
		var FragmentShaderSource = """
				#version 400 core
		
				out vec4 outColor;
				in float fromVertexShaderToFragmentShader;
				in vec4 gl_FragCoord;
				in vec3 color;
				uniform sampler2D brickTexture;
				
				void main()
				{
				  
				  outColor = vec4(fromVertexShaderToFragmentShader, color);
				}
				""";
		// vec4 texelColor = texture(brickTexture, vec2(0,0));
		// outColor = vec4(fromVertexShaderToFragmentShader, color)
		// outColor = vec4(fromVertexShaderToFragmentShader, 0.75, 0.0, 1.0);
		var hFragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(hFragmentShader, FragmentShaderSource);
		glCompileShader(hFragmentShader);
		if (glGetShaderi(hFragmentShader, GL_COMPILE_STATUS) != GL_TRUE)
			throw new Exception(glGetShaderInfoLog(hFragmentShader));

		// link shaders to a program
		var hProgram = glCreateProgram();
		glAttachShader(hProgram, hFragmentShader);
		glAttachShader(hProgram, hVertexShader);
		glLinkProgram(hProgram);
		if (glGetProgrami(hProgram, GL_LINK_STATUS) != GL_TRUE)
			throw new Exception(glGetProgramInfoLog(hProgram));

		// upload model vertices to a vbo

		//var triangleVertices = new float[] { 0.0f, 0f, 0.0f, 0.5f, 0f, 0.0f, 0.5f, -0.5f, 0.0f, 0f, -0.5f, 0.0f};
		var cubeVertices = new ArrayList<Vertex>();
		var cubeTriangles = new ArrayList<Tri>();
		MeshGenerator.addCube(cubeVertices, cubeTriangles,
				new Vector3(1f, 0f, 0f),
				new Vector3(0f, 1f, 0f),
				new Vector3(0f, 0f, 1f),
				new Vector3(1f, 1f, 0f),
				new Vector3(0f, 1f, 1f),
				new Vector3(1f, 0f, 1f));
		int len = cubeVertices.size() * 3;
		float[] verticesArray = new float[len];
		float[] colors = new float[len];
		float[] verticesST = new float[cubeVertices.size() * 2];
		int indLen = cubeTriangles.size() * 3;
		int[] triangleIndices = new int[indLen];

		int ind = 0;
		for (Tri triangle : cubeTriangles) {
			triangleIndices[ind++] = triangle.a();
			triangleIndices[ind++] = triangle.b();
			triangleIndices[ind++] = triangle.c();
		}
		int k = 0;
		int k2 = 0;
		for(int j = 0; j < cubeVertices.size(); j++) {
			var v = cubeVertices.get(j);
			colors[k] = v.color().x();
			verticesArray[k++] = v.position().x();
			colors[k] = v.color().y();
			verticesArray[k++] = v.position().y();
			colors[k] = v.color().z();
			verticesArray[k++] = v.position().z();
			verticesST[k2++] = v.st().x();
			verticesST[k2++] = v.st().y();
		}

//		var colors = new float[] {0f, 0.5f, 1f, 0f, 1f, 0.5f, 0.6f, 0.3f, 0.3f};
		var vboTriangleVertices = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboTriangleVertices);
		glBufferData(GL_ARRAY_BUFFER, verticesArray, GL_STATIC_DRAW);
		// upload model indices to a vbo
//		var triangleIndices = verticesIndexArray;//new int[] {0, 1, 2, 0, 3, 2};
		var vboTriangleIndices = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, triangleIndices, GL_STATIC_DRAW);
		var vboVertexColors = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboVertexColors);
		glBufferData(GL_ARRAY_BUFFER, colors, GL_STATIC_DRAW);
//		var vboVertexST = glGenBuffers();
//		glBindBuffer(GL_ARRAY_BUFFER, vboVertexST);
//		glBufferData(GL_ARRAY_BUFFER, verticesST, GL_STATIC_DRAW);
		// set up a vao
		var vaoTriangle = glGenVertexArrays();
		glBindVertexArray(vaoTriangle);
		var posAttribIndex = glGetAttribLocation(hProgram, "inPos");
		if (posAttribIndex != -1) {
			glEnableVertexAttribArray(posAttribIndex);
			glBindBuffer(GL_ARRAY_BUFFER, vboTriangleVertices);
			glVertexAttribPointer(posAttribIndex, 3, GL_FLOAT, false, 0, 0);
		}
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
		// also vao
		var colorAttribIndex = glGetAttribLocation(hProgram, "inColor");
		if (colorAttribIndex != -1) {
			glEnableVertexAttribArray(colorAttribIndex);
			glBindBuffer(GL_ARRAY_BUFFER, vboVertexColors);
			glVertexAttribPointer(colorAttribIndex, 3, GL_FLOAT, false, 0, 0);
		}
//		var stAttributeIndex = glGetAttribLocation(hProgram, "inSt");
//		if (stAttributeIndex != -1) {
//			glEnableVertexAttribArray(stAttributeIndex);
//			glBindBuffer(GL_ARRAY_BUFFER, vboVertexST);
//			glVertexAttribPointer(stAttributeIndex, 2, GL_FLOAT, false, 0, 0);
//		}
		// texture example
//		var texture = new Texture("../textures/MinecraftGlowstone.jpg", "jpg");
//		int hTexture = glGenTextures();
//		glBindTexture(GL_TEXTURE_2D, hTexture);
//		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
//		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
//		glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8, texture.getTexture().getWidth(), texture.getTexture().getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, texture.getTextureBuffer());
		// glBindTexture(GL_TEXTURE_2D, 0);
		// check for errors during all previous calls
		var error = glGetError();
		if (error != GL_NO_ERROR)
			throw new Exception(Integer.toString(error));


		float zNear = 0.1f;
		float zFar = 100f;
		Vector3 eye = new Vector3(0, 0, -4);
		Vector3 up = new Vector3(0, 1, 0);
		Vector3 lookAt = new Vector3(0, 0, 0);
		Matrix4x4 V = Matrix4x4.createLookAt(eye, lookAt, up);
		int[] width = new int[1];
		int[] height = new int[1];
		var matrix2 = Matrix4x4.createTranslation(2f, -2f, 1.5f);
		var matrix3 = Matrix4x4.createTranslation(-2f, 2f, 1.25f);
		// render loop
		var startTime = System.currentTimeMillis();
		while (!GLFW.glfwWindowShouldClose(hWindow)) {
			GLFW.glfwGetWindowSize(hWindow, width, height);
			float aspectRatio = width[0] / (float) height[0];
			Matrix4x4 P = Matrix4x4.createPerspectiveFieldOfView(1.5708f, aspectRatio, zNear, zFar);
			var mvp = V.multiply(P);
			var time = (float) (System.currentTimeMillis() - startTime) * 0.001f;
			// clear screen and z-buffer
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// switch to our shader
			glUseProgram(hProgram);

			// set texture
//			int tiuIndex = 1;
//			glActiveTexture(GL_TEXTURE0 + tiuIndex);
//			glBindTexture(GL_TEXTURE_2D, hTexture);
//			glUniform1i(glGetUniformLocation(hProgram, "brickTexture"), tiuIndex);
//			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);

			// set uniform values
			glUniform1f(glGetUniformLocation(hProgram, "inTime"), time);

			var someMatrix = (Matrix4x4.createRotationX((float) Math.cos(time))).multiply(Matrix4x4.createRotationY((float) Math.sin(time))).multiply(mvp);
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someMatrix.toArray());

			// render our model
			glBindVertexArray(vaoTriangle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
			glDrawElements(GL_TRIANGLES, triangleIndices.length, GL_UNSIGNED_INT, 0);
			// render our model 2
			var someOtherMatrix = Matrix4x4.createRotationX((float) Math.sin(time)).multiply(Matrix4x4.createRotationY((float) Math.cos(time)))
					.multiply(matrix2).multiply(mvp);
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someOtherMatrix.toArray());
			glBindVertexArray(vaoTriangle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
			glDrawElements(GL_TRIANGLES, triangleIndices.length, GL_UNSIGNED_INT, 0);
			// render our model 3
			var someOtherOtherMatrix = (Matrix4x4.createRotationX(1f + (float) Math.sin(0.7f + time))).multiply(Matrix4x4.createRotationY((float) Math.cos(1.3f + time)))
					.multiply(matrix3).multiply(mvp);
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someOtherOtherMatrix.toArray());
			glBindVertexArray(vaoTriangle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
			glDrawElements(GL_TRIANGLES, triangleIndices.length, GL_UNSIGNED_INT, 0);
			// display
			GLFW.glfwSwapBuffers(hWindow);
			GLFW.glfwPollEvents();

			error = glGetError();
			if (error != GL_NO_ERROR)
				throw new Exception(Integer.toString(error));
		}

		GLFW.glfwDestroyWindow(hWindow);
		GLFW.glfwTerminate();
	}
}
