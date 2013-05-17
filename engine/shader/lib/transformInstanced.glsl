#define SIZE 30
//object properties
uniform vec3 scaleArr[SIZE];
uniform vec3 translateArr[SIZE];
uniform vec3 translateOldArr[SIZE];
uniform mat3 rotationMatrices[SIZE];
uniform vec3 colorArr[SIZE];
uniform float interp;

mat4 getRotation(){
	mat4 R = mat4(	vec4(rotationMatrices[gl_InstanceID][0],0),
					vec4(rotationMatrices[gl_InstanceID][1],0),
					vec4(rotationMatrices[gl_InstanceID][2],0),
					vec4(0,0,0,1));
 	return R;
}

mat4 getSize(){
	mat4 S = mat4(	vec4(scaleArr[gl_InstanceID].x,0,0,0),
              		vec4(0,scaleArr[gl_InstanceID].y,0,0),
              		vec4(0,0,scaleArr[gl_InstanceID].z,0),
              		vec4(0,0,0,1) );
 	return S;
}

mat4 getTranslation(){
	vec3 translate  = mix(translateOldArr[gl_InstanceID],translateArr[gl_InstanceID],interp);
	mat4 T = mat4(	vec4(1,0,0,0),
              		vec4(0,1,0,0),
              		vec4(0,0,1,0),
              		vec4(translate,1));
    return T;
}
