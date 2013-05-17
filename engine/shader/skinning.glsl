uniform mat4 bones[100];
uniform int boneIndex;

void main()
{
	//color
	gl_FrontColor = gl_Color;
	//uv
	gl_TexCoord[0]= gl_MultiTexCoord0;
	//pos
	gl_Position = gl_ProjectionMatrix *  gl_ModelViewMatrix * bones[boneIndex] * gl_Vertex;
}
//fragment 


void main()
{
	gl_FragColor = gl_Color;
	gl_FragColor.a = 1.0;
}
















