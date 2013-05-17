
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

void saveGBuffer(){
	vec2 textureCoord = gl_TexCoord[0].xy;
	//vec2 textureCoord = 1-gl_TexCoord[0].xy;
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
		gl_FragData[0].rgb =mix(gl_FragData[0].rgb,textureCube(cubeMap, reflect(-viewVec,gl_FragData[1].rgb)).rgb,reflective*(dot(gl_FragData[1].rgb,normalize(viewVec))));
	
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