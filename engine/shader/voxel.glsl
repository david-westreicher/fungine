#version 120
#define SIZE 30
//object properties
uniform vec3 chunkPos;
uniform float scale;
uniform float time;

//displacement
uniform bool hasDisplacement = false;
uniform sampler2D displacementMap; 
uniform bool hasAirShader = false; 

varying vec3 normal;
varying vec4 pos;
varying vec3 viewVec;
uniform vec3 camPos;

const vec2 size = vec2(0.0,0.01);
const ivec3 off = ivec3(0,-1,-1);

void main()
{
	//color
	gl_FrontColor = gl_Color;
	//uv
	gl_TexCoord[0]= gl_MultiTexCoord0;
	//normal
	normal =  normalize(gl_Normal );
	//position
	pos = vec4((gl_Vertex.xyz+chunkPos)*scale,1 );
	viewVec = (camPos-pos.xyz);
	gl_Position = gl_ProjectionMatrix *  gl_ModelViewMatrix *   pos;
}


//fragment 
#version 120
//import lib/gbuffer.glsl
uniform sampler2D triplanar;
uniform sampler2D triplanarNorm;
const float triScale = 1.0;
void main()
{
	saveGBuffer(); 
	vec3 triTexCoord =gl_FragData[2].rgb;
	vec3 scales = vec3( abs(gl_FragData[1].rgb));
	scales/=(scales.x+scales.y+scales.z);
	//gl_FragData[0].rgb *= texture2D(triplanar,triTexCoord.xy*triScale)*scales.z+texture2D(triplanar,triTexCoord.xz*triScale)*scales.y+texture2D(triplanar,triTexCoord.yz*triScale)*scales.x;
	//gl_FragData[0].rgb*=((normal.xyz*vec3(0.5))+vec3(0.5));

		//vec3 normalMapNormal = texture2D(triplanarNorm,triTexCoord.xy*triScale)*scales.z+texture2D(triplanarNorm,triTexCoord.xz*triScale)*scales.y+texture2D(triplanarNorm,triTexCoord.yz*triScale)*scales.x-vec4(0.5);
		//normalMapNormal.b*=0.25;
		//gl_FragData[1].rgb = normalize(normal+normalMapNormal.rgb);
}
















