#version 120
#define SIZE 30
//object properties
uniform vec3 scaleArr[SIZE];
uniform vec3 translateArr[SIZE];
uniform vec3 translateOldArr[SIZE];
uniform mat3 rotationMatrices[SIZE];
uniform vec3 colorArr[SIZE];
uniform float interp;
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
	vec3 translate  = translateOldArr[gl_InstanceID]*(1-interp)+translateArr[gl_InstanceID]*(interp);
	mat4 T = mat4(	vec4(1,0,0,0),
              		vec4(0,1,0,0),
              		vec4(0,0,1,0),
              		vec4(translate,1));
    return T;
}


void main()
{
	mat4 rotationMatrix = getRotation();
	mat4 transformMatrix =getTranslation()*rotationMatrix*getSize();
	
	//color
	gl_FrontColor = gl_Color * vec4(colorArr[gl_InstanceID],1);
	
	//uv
	gl_TexCoord[0]= gl_MultiTexCoord0;
	
	//normal
	normal =  normalize(mat3(rotationMatrix) * gl_Normal );
	//vec4 glVertexPos = gl_Vertex;
	if(hasDisplacement){
		//gl_TexCoord[0].y*=-1;
		float scale = 0.05/sqrt(3);
		float s11 = length(texture2D(displacementMap,gl_TexCoord[0].xy))*scale*10;
	    float s01 = length(textureOffset(displacementMap, gl_TexCoord[0].xy,off.xy))*scale*10;
	    float s21 = length(textureOffset(displacementMap, gl_TexCoord[0].xy,off.zy))*scale*10;
	    float s10 = length(textureOffset(displacementMap, gl_TexCoord[0].xy,off.yx))*scale*10;
	    float s12 = length(textureOffset(displacementMap, gl_TexCoord[0].xy,off.yz))*scale*10;
	    vec3 va = normalize(vec3(size.x,s21-s11,size.y));
		vec3 vb = normalize(vec3(size.y,s12-s10,size.x));
	    vec3 dispNormal =  normalize(cross(va,vb));
	    dispNormal.b*=-1;
	    normal =normalize(normal+dispNormal);
		gl_Vertex.y+= s11/5-scale;
		//gl_TexCoord[0].y*=-1;
    } 
    
	//position
	pos = (transformMatrix * gl_Vertex);
	if(hasAirShader&&gl_TexCoord[0].y<0.1){
	pos.x+=sin(time/100+pos.x)/5;
	pos.z+=cos(time/200+pos.x+pos.z)/5;}
	viewVec = (camPos-pos.xyz);
	//trip shader
	/*float angle =pos.x/10 + mod(time,10000)/100.0;
	pos.y+=sin(angle);
	pos.z+=cos(angle);*/
	gl_Position = gl_ProjectionMatrix *  gl_ModelViewMatrix *   pos;

}


//fragment 
#version 120

uniform samplerCube cubeMap;
uniform bool hasTexture = false;
uniform sampler2D tex; 
uniform bool hasNormalMap = false;
uniform sampler2D normalMap;
uniform bool hasSpecMap = false;
uniform sampler2D specMap;
uniform bool hasMask = false;
uniform sampler2D maskMap;
uniform float shininess;
uniform float reflective=1.0;
varying vec3 normal;
varying vec4 pos;
varying vec3 viewVec;

void main()
{
	vec2 textureCoord = gl_TexCoord[0].xy;
	//textureCoord.y *= -1;
	if(hasMask && texture2D(maskMap,textureCoord).r<0.5)
		discard;
	
	// normal
	gl_FragData[1].rgb = normal;
	if(hasNormalMap){
		/*vec3 normalMapNormal = 2*texture2D(normalMap,textureCoord).rgb-vec3(1.0);
		normalMapNormal.r*=-1;
		gl_FragData[1].rgb = normalize(transInv*normalMapNormal);*/
		
		vec3 normalMapNormal = texture2D(normalMap,textureCoord).rgb-vec3(0.5);
		normalMapNormal.b*=0.25;
		gl_FragData[1].rgb = normalize(normal+normalMapNormal.rgb);
	}
	
	
	
	//color
	gl_FragData[0] = gl_Color;
	if(hasTexture){
		gl_FragData[0] *= texture2D(tex,textureCoord);
	}
	if(reflective>0)
		gl_FragData[0].rgb =mix(gl_FragData[0].rgb,textureCube(cubeMap, reflect(-viewVec,gl_FragData[1].rgb)).rgb,reflective);
	
	// position
	gl_FragData[2].rgb = pos.xyz;
	
	// specular
	gl_FragData[1].a = shininess;
	if(hasSpecMap){
		gl_FragData[2].a = texture2D(specMap,textureCoord).r*100.0/shininess;
	} else {
		gl_FragData[2].a = 1;
	}
	
}
















