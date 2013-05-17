#version 120
#define SIZE 30
uniform vec3 scaleArr[SIZE];
uniform vec3 translateArr[SIZE];
uniform vec3 translateOldArr[SIZE];
uniform mat3 rotationMatrices[SIZE];
uniform vec3 colorArr[SIZE];
uniform float interp;
uniform vec3 lightPos;


varying vec4 shadowPos,pos,light_position,eyeVec,glVertex;
varying vec3 normal;


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
	mat4 transformMatrix = getTransformMatrix();
	mat4 modelView = gl_ModelViewMatrix * transformMatrix;
	//mat4 modelView = gl_ModelViewMatrix;
	glVertex = gl_Vertex;
	//glVertex.y-=(length(glVertex.xz)*length(glVertex.xz));
	shadowPos = gl_TextureMatrix[7] *transformMatrix* glVertex;
	normal =  normalize( ( transpose(inverse(modelView))  * vec4(gl_Normal,1) ).xyz);
	//normal = normalize(gl_NormalMatrix * gl_Normal);
	light_position = gl_TextureMatrix[6]*vec4(lightPos,1);
	pos = modelView * glVertex;
	eyeVec = -pos;
	gl_Position = gl_ProjectionMatrix * pos;
	gl_FrontColor = gl_Color * vec4(colorArr[gl_InstanceID],1);
	//gl_FrontColor = gl_Color;
	gl_TexCoord[0]= gl_MultiTexCoord0;
	float distFromCam = length(pos)/100;
	gl_PointSize = sqrt(100000/(distFromCam*distFromCam*0.01+1));
	//gl_PointSize = 10;
}
//fragment
#version 120
uniform sampler2D shadowTexture;
varying vec4 shadowPos,pos,light_position,eyeVec,glVertex;
varying vec3 normal;


void main()
{
	vec4 shadowCoordinateWdivide = shadowPos / shadowPos.w ;
	//shadowCoordinateWdivide.z-=0.00001;
	float darkness = 0.1;
	float shadow =darkness;
	if(shadowCoordinateWdivide.s>0&&shadowCoordinateWdivide.s<1&&shadowCoordinateWdivide.t>0&&shadowCoordinateWdivide.t<1){
		float distanceFromLight =  texture2D(shadowTexture,shadowCoordinateWdivide.st).r;
		if (shadowPos.w > 0.0){
			float xDist = shadowCoordinateWdivide.s-0.5;
			float yDist = shadowCoordinateWdivide.t-0.5;
			float dist =clamp(sqrt(xDist*xDist+yDist*yDist)*2.0,0,1);
	 		if( distanceFromLight >= shadowCoordinateWdivide.z)
	 			shadow= 1-dist*(1-darkness);
	 			//shadow = darkness;
	 	}
	 }
	
	vec4 finalColor = gl_Color;
	vec4 diffuse = vec4(0.5);
	vec4 specularMaterial = vec4(0.1);
	vec3 L = normalize(light_position.xyz-pos.xyz).xyz;
	float lambertTerm = max(dot(normal, L),0);
	float shininess = 1;
	if(lambertTerm > 0){
		finalColor+= diffuse*lambertTerm;
		vec3 E = normalize(eyeVec.xyz);
		vec3 R = reflect(-L, normal);
		float specular = pow( max(dot(R, E), 0.0), 
			                 shininess );
		finalColor += specularMaterial*
						   specular;
	} else {
		shadow = darkness;
		//finalColor*=darkness;
	}
	
	
	gl_FragColor =finalColor*shadow;
	gl_FragColor.a =1.0;
	//gl_FragColor = vec4(length(light_position.xyz-pos.xyz)/5000);
	//gl_FragColor =shadow;
	//gl_FragColor = vec4(normal,1);
	//gl_FragColor = vec4(length(glVertex.xz));
	//gl_FragColor = gl_Color;
}
