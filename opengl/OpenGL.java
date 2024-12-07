package opengl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;

import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import vectors.*;

import java.util.ArrayList;

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
					gl_Position = vec4(inPos, 1.0) + vec4(sin(inTime) * 0.5, cos(inTime) * 0.5, 0.0, 0.0) * inMatrix;
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
				
				void main()
				{
				  outColor = vec4(fromVertexShaderToFragmentShader, color);
				}
				""";
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
				new Vector3(0f, 0.5f, 1f),
				new Vector3(0f, 1f, 0.5f),
				new Vector3(0.6f, 0.3f, 0.3f),
				new Vector3(1f, 0.5f, 0.5f),
				new Vector3(0f, 1f, 1f),
				new Vector3(1f, 0.5f, 1f));
		int len = cubeVertices.size() * 3;
		float[] verticesArray = new float[len];
		int[] verticesIndexArray = new int[len];
		float[] colors = new float[len];
		int i = 0;
		for (int j = 0; j < cubeVertices.size(); j++) {
			verticesIndexArray[i] = i;
			colors[i] = cubeVertices.get(j).color().x();
			verticesArray[i] = cubeVertices.get(j).worldCoordinates().x();
			i++;
			verticesIndexArray[i] = i;
			colors[i] = cubeVertices.get(j).color().y();
			verticesArray[i] = cubeVertices.get(j).worldCoordinates().y();
			i++;
			verticesIndexArray[i] = i;
			colors[i] = cubeVertices.get(j).color().z();
			verticesArray[i] = cubeVertices.get(j).worldCoordinates().z();
			i++;
		}
//		var colors = new float[] {0f, 0.5f, 1f, 0f, 1f, 0.5f, 0.6f, 0.3f, 0.3f};
		var vboTriangleVertices = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboTriangleVertices);
		glBufferData(GL_ARRAY_BUFFER, verticesArray, GL_STATIC_DRAW);

		// upload model indices to a vbo
		var triangleIndices = verticesIndexArray;//new int[] {0, 1, 2, 0, 3, 2};
		var vboTriangleIndices = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, triangleIndices, GL_STATIC_DRAW);
		var vboVertexColors = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboVertexColors);
		glBufferData(GL_ARRAY_BUFFER, colors, GL_STATIC_DRAW);
		// set up a vao
		var vaoTriangle = glGenVertexArrays();
		glBindVertexArray(vaoTriangle);
		var posAttribIndex = glGetAttribLocation(hProgram, "inPos");
		if (posAttribIndex != -1) {
			glEnableVertexAttribArray(posAttribIndex);
			glBindBuffer(GL_ARRAY_BUFFER, vboTriangleVertices);
			glVertexAttribPointer(posAttribIndex, 3, GL_FLOAT, false, 0, 0);
		}
		glBindVertexArray(vaoTriangle);
		var colorAttribIndex = glGetAttribLocation(hProgram, "inColor");
		if (colorAttribIndex != -1) {
			glEnableVertexAttribArray(colorAttribIndex);
			glBindBuffer(GL_ARRAY_BUFFER, vboVertexColors);
			glVertexAttribPointer(colorAttribIndex, 3, GL_FLOAT, false, 0, 0);
		}
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);

		// check for errors during all previous calls
		var error = glGetError();
		if (error != GL_NO_ERROR)
			throw new Exception(Integer.toString(error));

		// render loop
		var startTime = System.currentTimeMillis();
		while (!GLFW.glfwWindowShouldClose(hWindow)) {
			// clear screen and z-buffer
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// switch to our shader
			glUseProgram(hProgram);
			
			// set uniform values
			glUniform1f(glGetUniformLocation(hProgram, "inTime"), (float) (System.currentTimeMillis() - startTime) * 0.001f);

			var someMatrix = Matrix4x4.IDENTITY;
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someMatrix.toArray());
			
			// render our model
			glBindVertexArray(vaoTriangle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
			glDrawElements(GL_TRIANGLES, triangleIndices.length, GL_UNSIGNED_INT, 0);
			// render our model
			var someOtherMatrix = Matrix4x4.createScale(1.5f, 1.5f, 0f);
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someOtherMatrix.toArray());
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
