uniform vec3 scale;
uniform vec3 translate;
uniform vec3 translateOld;
uniform mat3 rotationMatrix;
uniform vec3 color;
uniform float interp;

mat4 getRotation(){
	mat4 R = mat4(	vec4(rotationMatrix[0],0),
					vec4(rotationMatrix[1],0),
					vec4(rotationMatrix[2],0),
					vec4(0,0,0,1));
 	return R;
}

mat4 getSize(){
	mat4 S = mat4(	vec4(scale.x,0,0,0),
              		vec4(0,scale.y,0,0),
              		vec4(0,0,scale.z,0),
              		vec4(0,0,0,1));
 	return S;
}

mat4 getTranslation(){
	vec3 translate  = mix(translateOld,translate,interp);
	mat4 T = mat4(	vec4(1,0,0,0),
              		vec4(0,1,0,0),
              		vec4(0,0,1,0),
              		vec4(translate,1));
    return T;
}