#version 120
varying vec3 viewVec;
void main(void)
{
	viewVec = vec3(gl_Vertex);
	gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix *gl_Vertex;
	gl_TexCoord[0]= gl_MultiTexCoord0;
}
//fragment 
#version 120
uniform samplerCube cubeMap;
varying vec3 viewVec;

void main()
{
	gl_FragColor = vec4(textureCube(cubeMap, viewVec));
	
}
















