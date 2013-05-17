void main(void)
{
	gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix *gl_Vertex;
	gl_TexCoord[0]= gl_MultiTexCoord0;
	gl_FrontColor = gl_Color;
}

//fragment
uniform sampler2D tex;
uniform float ambient;
void main()
{
   gl_FragColor = texture2D(tex,gl_TexCoord[0].xy);
   gl_FragColor.rgb *=ambient;
   //gl_FragColor.a = 1.0;
}
