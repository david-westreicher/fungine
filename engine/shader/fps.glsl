#version 330 core
layout(location = 0) in vec3 vertex;
out vec2 uv;
uniform mat4 modelviewprojection;

void main(){
	uv = vertex.xy+0.5;
	gl_Position = modelviewprojection*vec4(vertex,1.0);
}

//fragment
#version 330 core
out vec4 color;
in vec2 uv;
uniform sampler2D fpsTex;
uniform float translateX =0;
uniform float colorScale =0;
 
void main(){
	vec2 trans = vec2(uv);
	trans.x+=translateX;
	color = vec4(0,0,0,0)*colorScale;//50;
	for(float lvl = 1;lvl<15;lvl+=1){
		color += textureLod(fpsTex,trans,lvl);
	}
	color/=2;
	//color = textureLod(fpsTex,trans,14);
	//color.a = 1;
}