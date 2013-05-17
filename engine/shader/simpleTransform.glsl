uniform vec3 size;
uniform vec3 pos;

vec4 applyTransform(vec4 vertex){
	mat4 S = mat4(	vec4(size.x,0,0,0),
              		vec4(0,size.y,0,0),
              		vec4(0,0,size.z,0),
              		vec4(0,0,0,1) );
	mat4 T = mat4(	vec4(1,0,0,0),
              		vec4(0,1,0,0),
              		vec4(0,0,1,0),
              		vec4(pos,1));
	return T*S*vertex;
}

void main(void)
{
	gl_Position = gl_ModelViewProjectionMatrix * applyTransform(gl_Vertex);
}

//fragment

void main(void)
{ 
  gl_FragColor =vec4(1,1,1,1);
}