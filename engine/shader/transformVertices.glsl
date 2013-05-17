#version 120
#define SIZE 30
//object properties
uniform vec3 scaleArr[SIZE];
uniform vec3 translateArr[SIZE];
uniform vec3 translateOldArr[SIZE];
uniform mat3 rotationMatrices[SIZE];
uniform float interp;
uniform float time;
uniform bool hasAirShader = false; 


mat4 getTransformMatrix(){
	vec3 scale = scaleArr[gl_InstanceID];
	vec3 translate  = translateOldArr[gl_InstanceID]*(1-interp)+translateArr[gl_InstanceID]*(interp);
	mat4 R = mat4(	vec4(rotationMatrices[gl_InstanceID][0],0),
					vec4(rotationMatrices[gl_InstanceID][1],0),
					vec4(rotationMatrices[gl_InstanceID][2],0),
					vec4(0,0,0,1));
	mat4 S = mat4(	vec4(scale.x,0,0,0),
              		vec4(0,scale.y,0,0),
              		vec4(0,0,scale.z,0),
              		vec4(0,0,0,1) );
	mat4 T = mat4(	vec4(1,0,0,0),
              		vec4(0,1,0,0),
              		vec4(0,0,1,0),
              		vec4(translate,1));
 	return T*R*S;
}


void main()
{
	//position
	vec4 pos = getTransformMatrix() * gl_Vertex;
	if(hasAirShader&&gl_MultiTexCoord0.y<0.1){
	pos.x+=sin(time/100+pos.x)/5;
	pos.z+=cos(time/200+pos.x+pos.z)/5;}
	//trip shader
	/*float angle =pos.x/10 + mod(time,10000)/100.0;
	pos.y+=sin(angle);
	pos.z+=cos(angle);*/
	gl_TexCoord[0] = gl_MultiTexCoord0;
	gl_Position = gl_ProjectionMatrix *  gl_ModelViewMatrix * pos;

}
//fragment 
#version 120

uniform bool hasMask = false;
uniform sampler2D maskMap;
void main()
{
	if(hasMask && texture2D(maskMap,gl_TexCoord[0].xy).r<0.5)
		discard;
	gl_FragColor = vec4(1,0,1,1);
}
















