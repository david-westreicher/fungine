#version 120
#define SIZE 30
//object properties
uniform vec3 chunkPos;
uniform float scale;

void main()
{
	//uv
	gl_TexCoord[0]= gl_MultiTexCoord0;
	//position
	vec4 pos = vec4((gl_Vertex.xyz+chunkPos)*scale,1 );
	gl_Position = gl_ProjectionMatrix *  gl_ModelViewMatrix *   pos;
}


//fragment 
#version 120

void main()
{
	gl_FragColor = vec4(1);
}
















