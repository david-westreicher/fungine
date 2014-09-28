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

vec3 tonemap(vec3 texColor)
{
	texColor *= 4;  // Hardcoded Exposure Adjustment
	vec3 x = max(vec3(0,0,0),texColor-0.004);
	vec3 retColor = (x*(6.2*x+.5))/(x*(6.2*x+1.7)+0.06);
	return retColor;
}

void main(){
	vec2 trans = vec2(uv);
	trans.x+=translateX;
	color = vec4(0,0,0,0);//50;
	for(int lvl = 0;lvl<12;lvl+=1){
		color += textureLod(fpsTex,trans,lvl);
	}
	//color/=2;
	//if(length(color.rgb)>sqrt(3)){
		//color = vec4(0,0,0,0);
		//color.rgb = tonemap(color.rgb);
	//}
	//}
	//}
	//color = textureLod(fpsTex,trans,0);
	//color.a = 1;
}