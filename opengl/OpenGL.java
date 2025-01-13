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

import java.nio.ByteBuffer;
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
		// Post Processing Shaders:
		var PPVertexShaderSource = """
				#version 400 core
				in vec3 aPos;
				in vec2 aTexCoords;

				out vec2 TexCoords;

				void main()
				{
				    TexCoords = aTexCoords;

				    // Standard vertex position transformation (pass through)
				    gl_Position = vec4(aPos, 1.0);
				}
				""";
		var hPPVertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(hPPVertexShader, PPVertexShaderSource);
		glCompileShader(hPPVertexShader);
		if (glGetShaderi(hPPVertexShader, GL_COMPILE_STATUS) != GL_TRUE)
			throw new Exception(glGetShaderInfoLog(hPPVertexShader));

		var PPFragmentShaderSource = """
				#version 400 core
				in vec2 TexCoords;
				out vec4 outColor;
				uniform sampler2D screenTexture;

				void main()
				{
					outColor = texture(screenTexture, TexCoords);
					// reduce blue light
					outColor = vec4(outColor.x, outColor.y, outColor.z / 3, outColor.w);
				}
				""";
		var hPPFragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(hPPFragmentShader, PPFragmentShaderSource);
		glCompileShader(hPPFragmentShader);
		if (glGetShaderi(hPPFragmentShader, GL_COMPILE_STATUS) != GL_TRUE)
			throw new Exception(glGetShaderInfoLog(hPPFragmentShader));

		var hPPProgram = glCreateProgram();
		glAttachShader(hPPProgram, hPPFragmentShader);
		glAttachShader(hPPProgram, hPPVertexShader);
		glLinkProgram(hPPProgram);
		if (glGetProgrami(hPPProgram, GL_LINK_STATUS) != GL_TRUE)
			throw new Exception(glGetProgramInfoLog(hPPProgram));

		// see https://www.khronos.org/opengl/wiki/Vertex_Shader
		var VertexShaderSource = """
				#version 400 core
								
				uniform float inTime;
				uniform mat4 inMatrix;
				uniform mat4 transfM;
				uniform mat4 modelMatrix;
				out float fromVertexShaderToFragmentShader;
				out vec3 color;
				out vec3 fragNormal;
				out vec3 fragPos;
				out vec2 st;
				in vec3 inPos;
				in vec3 inColor;
				in vec3 inNormal;
				in vec2 inSt;
								
				void main()
				{
					vec4 movement = vec4(sin(inTime) * 0.5, cos(inTime) * 0.5, 0.0, 0.0);
					vec4 worldPos = modelMatrix * vec4(inPos, 1.0);
				    fragPos = gl_Position.xyz;
				    fragNormal = normalize(mat3(transfM) * inNormal);
					color = inColor;
					gl_Position = inMatrix * vec4(inPos, 1.0);
					st = inSt;
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
				in vec3 fragNormal;
				in vec3 fragPos;
				in vec2 st;
				uniform vec3 lightPos;
				uniform vec3 viewPos;
				uniform vec3 lightColor;
				uniform sampler2D textureT;
				uniform float phongK;
				uniform int hasTexture;
				
				void main()
				{
				  vec3 textureColor = color;
				  if(hasTexture == 1) {
				  	textureColor = texture(textureT, st).xyz;
				  }
				  vec3 norm = normalize(fragNormal);
				  vec3 lightDir = normalize(lightPos - fragPos);
				  vec3 viewDir = normalize(viewPos - fragPos);
				  float diff = max(dot(norm, lightDir), 0.0);
				  vec3 reflectDir = reflect(-lightDir, norm);
				  float spec = pow(max(dot(viewDir, reflectDir), 0.0), phongK);
				  vec3 ambient = 0.1 * lightColor;
				  vec3 diffuse = diff * lightColor;
				  vec3 specular = spec * lightColor;
				  outColor = vec4((ambient + diffuse + specular) * textureColor, 1.0);
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



		int[] width = new int[1];
		int[] height = new int[1];
		GLFW.glfwGetWindowSize(hWindow, width, height);

		// upload model vertices to a vbo

		//var triangleVertices = new float[] { 0.0f, 0f, 0.0f, 0.5f, 0f, 0.0f, 0.5f, -0.5f, 0.0f, 0f, -0.5f, 0.0f};
		var cubeVertices = new ArrayList<Vertex>();
		var cubeTriangles = new ArrayList<Tri>();
		// MeshGenerator.addSphere(cubeVertices, cubeTriangles, 5, new Vector3(0.45f, 0.35f, 0.25f));
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
		float[] vertexNormals = new float[len * 3];
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
			var normal = v.normal();
			vertexNormals[k] = normal.x();
			colors[k] = v.color().x();
			verticesArray[k++] = v.position().x();
			vertexNormals[k] = normal.y();
			colors[k] = v.color().y();
			verticesArray[k++] = v.position().y();
			vertexNormals[k] = normal.z();
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
		var vboVertexNormals = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboVertexNormals);
		glBufferData(GL_ARRAY_BUFFER, vertexNormals, GL_STATIC_DRAW);
		var vboVertexST = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboVertexST);
		glBufferData(GL_ARRAY_BUFFER, verticesST, GL_STATIC_DRAW);
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
		var normalAttribIndex = glGetAttribLocation(hProgram, "inNormal");
		if (normalAttribIndex != -1) {
			glEnableVertexAttribArray(normalAttribIndex);
			glBindBuffer(GL_ARRAY_BUFFER, vboVertexNormals);
			glVertexAttribPointer(normalAttribIndex, 3, GL_FLOAT, false, 0, 0);
		}

		var stAttributeIndex = glGetAttribLocation(hProgram, "inSt");
		if (stAttributeIndex != -1) {
			glEnableVertexAttribArray(stAttributeIndex);
			glBindBuffer(GL_ARRAY_BUFFER, vboVertexST);
			glVertexAttribPointer(stAttributeIndex, 2, GL_FLOAT, false, 0, 0);
		}
		// Quad setup
		float[] quadVertices = {
				// Positions    // TexCoords
				-1.0f,  1.0f,  0.0f, 1.0f, // Top-left
				-1.0f, -1.0f,  0.0f, 0.0f, // Bottom-left
				1.0f, -1.0f,  1.0f, 0.0f, // Bottom-right
				1.0f,  1.0f,  1.0f, 1.0f  // Top-right
		};
		int[] quadIndices = {
				0, 1, 2, // First triangle
				0, 2, 3  // Second triangle
		};
		int quadVAO = glGenVertexArrays();
		int quadVBO = glGenBuffers();
		int quadEBO = glGenBuffers();

		glBindVertexArray(quadVAO);

		glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
		glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, quadIndices, GL_STATIC_DRAW);

		int aPosLocation = glGetAttribLocation(hPPProgram, "aPos");
		if (aPosLocation != -1) {
			glVertexAttribPointer(aPosLocation, 2, GL_FLOAT, false, 4 * Float.BYTES, 0); // First 2 floats are position
			glEnableVertexAttribArray(aPosLocation);
		}

		int aTexCoordsLocation = glGetAttribLocation(hPPProgram, "aTexCoords");
		if (aTexCoordsLocation != -1) {
			glVertexAttribPointer(aTexCoordsLocation, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES); // Next 2 floats are texcoords
			glEnableVertexAttribArray(aTexCoordsLocation);
		}
		// Unbind VAO
		glBindVertexArray(0);

		// main texture example
		var texture = new Texture("./textures/MinecraftGlowstone.jpg");
		int hTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, hTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, texture.getTexture().getWidth(), texture.getTexture().getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, texture.getTextureBuffer());
		glGenerateMipmap(GL_TEXTURE_2D);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);

		// texture for fbo
		int fboWidth = width[0], fboHeight = height[0];
		int fboTextureId = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, fboTextureId);
		glTexParameteri(GL_TEXTURE_2D,
				GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D,
				GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
				GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
				GL_CLAMP_TO_EDGE);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
				fboWidth, fboHeight, 0, GL_RGBA,
				GL_UNSIGNED_BYTE, 0);
		// allocate renderbuffer
		int rboId = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, rboId);
		glRenderbufferStorage(GL_RENDERBUFFER,
				GL_DEPTH_COMPONENT,
				fboWidth, fboHeight);
		// setup fbo
		int fboId = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fboId);
		glFramebufferTexture2D(GL_FRAMEBUFFER,
				GL_COLOR_ATTACHMENT0,
				GL_TEXTURE_2D,
				fboTextureId,
				0);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER,
				GL_DEPTH_ATTACHMENT,
				GL_RENDERBUFFER,
				rboId);
		assert(glCheckFramebufferStatus(GL_FRAMEBUFFER)
				== GL_FRAMEBUFFER_COMPLETE);

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
		Vector3 lightPos = new Vector3(0, 10, -2);
		var matrix2 = Matrix4x4.createTranslation(2f, -2f, 1.5f);
		var matrix3 = Matrix4x4.createTranslation(-2f, 2f, 1.25f);
		float phongK =  8.0f;
		// render loop
		var startTime = System.currentTimeMillis();
		while (!GLFW.glfwWindowShouldClose(hWindow)) {
//			glUseProgram(hProgram);
//			glEnable(GL_DEPTH_TEST);
//			glDepthFunc(GL_LESS);
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
			int tiuIndex = 1;
			glActiveTexture(GL_TEXTURE0 + tiuIndex);
			glBindTexture(GL_TEXTURE_2D, hTexture);
			glUniform1i(glGetUniformLocation(hProgram, "textureT"), tiuIndex);
			glUniform1i(glGetUniformLocation(hProgram, "hasTexture"), 1);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			// set uniform values
			glUniform3f(glGetUniformLocation(hProgram, "lightPos"), lightPos.x(), lightPos.y(), lightPos.z());
			glUniform3f(glGetUniformLocation(hProgram, "viewPos"), eye.x(), eye.y(), eye.z());
			glUniform3f(glGetUniformLocation(hProgram, "lightColor"), 1.0f, 1.0f, 1.0f);
			glUniform1f(glGetUniformLocation(hProgram, "inTime"), time);
			glUniform1f(glGetUniformLocation(hProgram, "phongK"), phongK);

			// setup to render into fbo
			glBindFramebuffer(GL_FRAMEBUFFER, fboId);
			glViewport(0, 0, fboWidth, fboHeight);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// rendering objects:
			var m = (Matrix4x4.createRotationX((float) Math.cos(time))).multiply(Matrix4x4.createRotationY((float) Math.sin(time)));
			var someMatrix = m.multiply(mvp);
			var transfM = Matrix4x4.transpose(Matrix4x4.invert(m)).multiply(m.getDeterminant());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someMatrix.toArray());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "modelMatrix"), false, m.toArray());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "transfM"), false, transfM.toArray());
			// render our model
			glBindVertexArray(vaoTriangle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
			glDrawElements(GL_TRIANGLES, triangleIndices.length, GL_UNSIGNED_INT, 0);
			// render our model 2
			m = Matrix4x4.createRotationX((float) Math.sin(time)).multiply(Matrix4x4.createRotationY((float) Math.cos(time)))
					.multiply(matrix2);
			var someOtherMatrix = m.multiply(mvp);
			transfM = Matrix4x4.transpose(Matrix4x4.invert(m)).multiply(m.getDeterminant());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someOtherMatrix.toArray());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "modelMatrix"), false, m.toArray());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "transfM"), false, transfM.toArray());
			glBindVertexArray(vaoTriangle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
			glDrawElements(GL_TRIANGLES, triangleIndices.length, GL_UNSIGNED_INT, 0);
			// render our model 3
			m = (Matrix4x4.createRotationX(1f + (float) Math.sin(0.7f + time))).multiply(Matrix4x4.createRotationY((float) Math.cos(1.3f + time)))
					.multiply(matrix3);
			var someOtherOtherMatrix = m.multiply(mvp);
			transfM = Matrix4x4.transpose(Matrix4x4.invert(m)).multiply(m.getDeterminant());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "inMatrix"), false, someOtherOtherMatrix.toArray());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "modelMatrix"), false, m.toArray());
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "transfM"), false, transfM.toArray());
			glBindVertexArray(vaoTriangle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
			glDrawElements(GL_TRIANGLES, triangleIndices.length, GL_UNSIGNED_INT, 0);
			// end of rendering objects
			// switch to post-processing
			glUseProgram(hPPProgram);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, fboTextureId);
			glUniform1i(glGetUniformLocation(hPPProgram, "screenTexture"), 0);
			// render to screen
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			glViewport(0, 0, width[0], height[0]);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glBindVertexArray(quadVAO);
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
			glBindVertexArray(0);

			// display
			GLFW.glfwSwapBuffers(hWindow);
			GLFW.glfwPollEvents();
			// switch back
			error = glGetError();
			if (error != GL_NO_ERROR)
				throw new Exception(Integer.toString(error));
		}

		GLFW.glfwDestroyWindow(hWindow);
		GLFW.glfwTerminate();
	}
}
