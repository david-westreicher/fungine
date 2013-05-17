#define SIZE 10
uniform vec3 scaleArr[SIZE];
uniform vec3 translateArr[SIZE];
uniform vec3 translateOldArr[SIZE];
uniform mat3 rotationMatrices[SIZE];
uniform vec3 colorArr[SIZE];
uniform float interp;
uniform mat4 bones[10];
attribute vec4 weights;
attribute vec4 boneIndices;

vec4 applyTransform(vec4 vertex){
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
	return T*R*S*vertex;
}
const vec3 boneCols[] = vec3[3](vec3(1,0,0),vec3(0,1,0),vec3(0,0,1));

void main(void)
{
	vec4 pos = gl_Vertex;
	pos += vec4(0,0.5,0,0);
	vec3 col = vec3(0);
	if(weights.x>0){
		col+=weights.x*boneCols[int(boneIndices.x)];
		pos+=bones[int(boneIndices.x)]*(gl_Vertex*weights.x);
		if(weights.y>0){
			col+=weights.y*boneCols[int(boneIndices.y)];
			pos+=bones[int(boneIndices.y)]*(gl_Vertex*weights.y);
		}
	}
	gl_Position = gl_ModelViewProjectionMatrix * applyTransform(pos);
	
	gl_FrontColor = vec4(col,1);//gl_Color * vec4(colorArr[gl_InstanceID],1)*weights*boneIndices;
	gl_TexCoord[0]= gl_MultiTexCoord0;
}

//fragment


// Scene buffer
uniform sampler2D tex; 


void main(void)
{ 
  gl_FragColor =vec4(gl_TexCoord[0].xy,0,1);
  gl_FragColor =texture2D(tex,gl_TexCoord[0].xy)*gl_Color;
  gl_FragColor =gl_Color;
 // gl_FragColor =gl_Color;
}