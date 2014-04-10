#version 330 core
layout(location = 0) in vec2 uv;
layout(location = 1) in vec3 vertex;
layout(location = 2) in vec3 normalasd;
out vec3 normalOut;
out vec2 uvOut;
uniform mat4 modelviewprojection;
uniform float textureSize;

void main(){
	normalOut = normalasd;
	uvOut = uv/textureSize;
    gl_Position = modelviewprojection*vec4(vertex,1.0);
}

//fragment
#version 330 core
out vec4 color;
in vec2 uvOut;
in vec3 normalOut;
uniform sampler2D giMap;
uniform float textureSize;
 
void main(){
    //color = vec4((normalOut+1.0)/2.0,1);
    //color = vec4(texture(),1);
    color = texture(giMap,uvOut);
}