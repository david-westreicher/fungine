uniform bool isFullScreen;
uniform vec3 lightPos;
uniform float lightRadius;


vec4 applyTransform(vec4 vertex){
	mat4 S = mat4(	vec4(lightRadius*2.1,0,0,0),
              		vec4(0,lightRadius*2.1,0,0),
              		vec4(0,0,lightRadius*2.1,0),
              		vec4(0,0,0,1) );
	mat4 T = mat4(	vec4(1,0,0,0),
              		vec4(0,1,0,0),
              		vec4(0,0,1,0),
              		vec4(lightPos,1));
	return T*S*vertex;
}

void main(void)
{
	//gl_Vertex = gl_Vertex+vec4(0,0,-0.5,0);
	if(isFullScreen)
		gl_Position = gl_ModelViewProjectionMatrix * applyTransform(gl_Vertex);
	else
		gl_Position = gl_ModelViewProjectionMatrix *gl_Vertex;
	gl_TexCoord[0]= gl_MultiTexCoord0;
}

//fragment

#version 120

uniform sampler2D diff;
uniform sampler2D normal;
uniform sampler2D position;
uniform sampler2DShadow shadowMap;
uniform vec3 camPos;
uniform vec3 lightPos;
uniform float lightRadius;
uniform vec3 lightColor;
uniform bool isFullScreen;
uniform bool hasShadowMap;
uniform bool hasFallof;
uniform float width;
uniform float height;

	vec4 ShadowCoord;
	
	// This define the value to move one pixel left or right
	uniform float pixelOffset=1.0/1024.0 ;
	
	const bool pcf = true;
float lookup( vec2 offSet)
	{
		// Values are multiplied by ShadowCoord.w because shadow2DProj does a W division for us.
		vec4 sCoord = ShadowCoord + vec4(vec2(offSet)*pixelOffset * ShadowCoord.w, 0.00, 0.0);
		float shadowVal = shadow2DProj(shadowMap, sCoord).r;
		return shadowVal;
	}
void main()
{

   
   vec2	texCoord = vec2(gl_FragCoord.x/width,gl_FragCoord.y/height);
	vec4 diffVec = texture2D(diff,texCoord);
	if(diffVec.a<1.0)
		discard;
	
	
   vec3 Mdiff = diffVec.rgb;
	
   if(!isFullScreen)
     texCoord = gl_TexCoord[0].xy;
   vec4 pos = texture2D(position,texCoord);
   
   float lightDist = distance(pos.xyz,lightPos);
   
   		
   if(lightDist>lightRadius)
   	discard;
    
   vec3 Spos = lightPos;
   vec3 p = pos.xyz;
   vec4 normal = texture2D(normal,texCoord);
   vec3 n = normal.xyz;
   
   vec3 l = Spos - p; // light vector
   if(dot(l,n)<0)
     discard;
     
     
      
   //shadow
   float shadow = 0.0;
   if(hasShadowMap){
	   vec4 shadowPos = gl_TextureMatrix[7] *vec4(pos.xyz,1);
	   ShadowCoord = shadowPos;
	   vec2 shadowWDivide = ShadowCoord.xy/ShadowCoord.w;
	   
	   if(shadowWDivide.s>0&&shadowWDivide.s<1&&shadowWDivide.t>0&&shadowWDivide.t<1&&ShadowCoord.w>1.0){
	  	 	if(pcf){
		  	 	float x,y;
				for (y = -1.5 ; y <=1.5 ; y+=1.0)
					for (x = -1.5 ; x <=1.5 ; x+=1.0)
						shadow += lookup(vec2(x,y));
				shadow /= 16.0 ;
			}else{
				shadow = lookup(vec2(0,0));
			}
			if(hasFallof){
				float dist =clamp(length(shadowWDivide.st - vec2(0.5))*2.0,0,1);
				shadow*=1-dist;
			}
	  	}
	// }
   }else{
		shadow = 1.0;
   }
  // if(isnan(shadow))
  //  discard;
  
    
   vec3 Scolor = lightColor;
   float Mshi = normal.a;
   vec3 Mspec = vec3(pos.a);
   //Mspec=exp(Mspec*19)-1;
   float Sradius = lightRadius;
   vec3 v = camPos-p; // view vector
   vec3 h = normalize(v + l); // half vector
    
   // attenuation (equation 2)
   float att = clamp(1.0 - length(l)/Sradius,0,1);
   vec3 Idiff = vec3(0);
   vec3 Ispec = vec3(0);
   Idiff = clamp((dot(normalize(l),n))*Mdiff*Scolor,0,1);
   if(Mshi!=0)
     Ispec = pow(clamp(dot(h,n),0,1),Mshi)*Mspec*Scolor;
   	gl_FragColor = vec4(att*(Idiff + Ispec)*shadow,1);
}
