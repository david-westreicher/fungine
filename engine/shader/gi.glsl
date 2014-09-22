#version 330 core
layout(location = 0) in vec2 uv;
layout(location = 1) in vec3 vertex;
layout(location = 2) in vec3 normalasd;
out vec3 normalOut;
out vec2 uvOut;
uniform mat4 modelviewprojection;
uniform float textureSize;
//uniform vec3 camPos;
//const float radiusSQ = 25.0;

void main(){
	normalOut = normalasd;
	uvOut = uv/textureSize;
	//vec3 pa = vertex-camPos;
	//vec3 vertex2 = pa*(radiusSQ/length(pa))+camPos;
	gl_Position = modelviewprojection*vec4(vertex,1.0);
}

//fragment
#version 330 core
out vec4 color;
in vec2 uvOut;
in vec3 normalOut;
uniform sampler2D giMap;
uniform float textureSize;
uniform float colorscale;
 
void main(){
	// color = vec4((normalOut+1.0)/2.0,1);
	////color = vec4(texture(),1);
	color = texture(giMap,uvOut)*colorscale;
	color.a = 1;
}