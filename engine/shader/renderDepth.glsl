void main(void)
{
	gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix *gl_Vertex; 
	gl_FrontColor = gl_Color;
	gl_TexCoord[0]= gl_MultiTexCoord0;
}

//fragment


// Scene buffer

//uniform sampler2D tex; 
uniform sampler2D depth; 
uniform float zNear;
uniform float zFar;

float LinearizeDepth(float z)
{
  return (2.0 * zNear) / (zFar + zNear - z * (zFar - zNear));
}

void main()
{
 
  //gl_FragColor.rgb = vec3(texture2D(depth,gl_TexCoord[0].xy).a);
  //	gl_FragColor.a = 1.0;
   //gl_FragColor = vec4(0.5);
 gl_FragColor =vec4(LinearizeDepth(texture2D(depth,gl_TexCoord[0].xy).r));
 //gl_FragColor = texture2D(depth,gl_TexCoord[0].xy).r/3;
 gl_FragColor.a = 1;
}