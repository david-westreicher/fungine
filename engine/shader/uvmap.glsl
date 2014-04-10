#version 330 core
layout(location = 0) in vec2 uv;
layout(location = 1) in vec3 vertex;
layout(location = 2) in vec3 normalasd;
out vec3 posOut;
out vec3 normalOut;
uniform mat4 modelviewprojection;
uniform float anim;

void main(){
	vec3 pos1 = vec3(uv,0);
	vec3 pos2 = vertex;
	vec3 pos = mix(pos1,pos2,anim);
	posOut = pos2;
	normalOut = normalasd;
    gl_Position = modelviewprojection*vec4(pos,1.0);
}

//fragment
#version 330 core
out vec4 color;
in vec3 posOut;
in vec3 normalOut;
 
void main(){
    //color = vec4((normalOut+1.0)/2.0,1);
    color = vec4(posOut/10.0,1);
   //	color = vec4(0,0,0,1);
}