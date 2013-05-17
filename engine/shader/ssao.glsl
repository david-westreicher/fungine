void main(void)
{
	gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix *gl_Vertex;
	gl_TexCoord[0]= gl_MultiTexCoord0;
	gl_FrontColor = gl_Color;
}

//fragment
uniform float ambient;
uniform sampler2D gnormals;
uniform sampler2D gpos;
uniform sampler2D gdiffuse;
uniform sampler2D grandom;
uniform vec3 camPos;
uniform mat3 camRotation;
uniform float ssaoStrength;
uniform float width;
uniform float height;

vec3 readNormal(in vec2 coord)  
{  
     return normalize(texture2D(gnormals, coord).xyz);  
}

vec4 applyTransform(vec4 vertex){
		mat4 R = mat4(	vec4(camRotation[0],0),
					vec4(camRotation[1],0),
					vec4(camRotation[2],0),
					vec4(0,0,0,1));
	mat4 T = mat4(	vec4(1,0,0,0),
              		vec4(0,1,0,0),
              		vec4(0,0,1,0),
              		vec4(camPos,1));
	return T*R*vertex;
}

vec3 posFromDepth(vec2 coord){
     return texture2D(gpos, coord).xyz;
}
    //Ambient Occlusion form factor:
    float aoFF(in vec3 ddiff,in vec3 cnorm, in float c1, in float c2){
          vec3 vv = normalize(ddiff);
          float rd = length(ddiff);
          return (1.0-clamp(dot(readNormal(gl_TexCoord[0].xy+vec2(c1,c2)),-vv),0.0,1.0)) *
           clamp(dot( cnorm,vv ),0.0,1.0)* 
                 (1.0 - 1.0/sqrt(1.0/(rd*rd) + 1.0));
    }
    //GI form factor:
    float giFF(in vec3 ddiff,in vec3 cnorm, in float c1, in float c2){
          vec3 vv = normalize(ddiff);
          float rd = length(ddiff);
          return 1.0*clamp(dot(readNormal(gl_TexCoord[0].xy+vec2(c1,c2)),-vv),0.0,1.0)*
                     clamp(dot( cnorm,vv ),0.0,1.0)/
                     (rd*rd+1.0);  
    }

void main()
{
    //read current normal,position and color.
    vec3 n = readNormal(gl_TexCoord[0].st);
    vec3 p = posFromDepth(gl_TexCoord[0].st);
    vec4 cola = texture2D(gdiffuse, gl_TexCoord[0].st);
    if(cola.a==0)
    	discard;
    vec3 col = cola.rgb;

    //randomization texture
    vec2 fres = vec2(width/1280.0/1,height/1280.0/1);
    vec3 random = texture2D(grandom, gl_TexCoord[0].st*fres.xy).rgb;
    random = random*2.0-vec3(1.0);

    //initialize variables:
    float ao = 0.0;
   // vec3 gi = vec3(0.0,0.0,0.0);
    float incx = 5/width;
    float incy = 5/height;
    float pw = incx;
    float ph = incy;
    float cdepth = distance(camPos,p)/10;

    //3 rounds of 8 samples each. 
    for(float i=0.0; i<2.0; ++i) 
    {
       float npw = (pw+0.001*random.x)/cdepth;
       float nph = (ph+0.001*random.y)/cdepth;

       vec3 ddiff = posFromDepth(gl_TexCoord[0].st+vec2(npw,nph))-p;
       vec3 ddiff2 = posFromDepth(gl_TexCoord[0].st+vec2(npw,-nph))-p;
       vec3 ddiff3 = posFromDepth(gl_TexCoord[0].st+vec2(-npw,nph))-p;
       vec3 ddiff4 = posFromDepth(gl_TexCoord[0].st+vec2(-npw,-nph))-p;
       vec3 ddiff5 = posFromDepth(gl_TexCoord[0].st+vec2(0,nph))-p;
       vec3 ddiff6 = posFromDepth(gl_TexCoord[0].st+vec2(0,-nph))-p;
       vec3 ddiff7 = posFromDepth(gl_TexCoord[0].st+vec2(npw,0))-p;
       vec3 ddiff8 = posFromDepth(gl_TexCoord[0].st+vec2(-npw,0))-p;

       ao+=  aoFF(ddiff,n,npw,nph);
       ao+=  aoFF(ddiff2,n,npw,-nph);
       ao+=  aoFF(ddiff3,n,-npw,nph);
       ao+=  aoFF(ddiff4,n,-npw,-nph);
       ao+=  aoFF(ddiff5,n,0,nph);
       ao+=  aoFF(ddiff6,n,0,-nph);
       ao+=  aoFF(ddiff7,n,npw,0);
       ao+=  aoFF(ddiff8,n,-npw,0);

       /*gi+=  giFF(ddiff,n,npw,nph)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(npw,nph)).rgb;
       gi+=  giFF(ddiff2,n,npw,-nph)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(npw,-nph)).rgb;
       gi+=  giFF(ddiff3,n,-npw,nph)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(-npw,nph)).rgb;
       gi+=  giFF(ddiff4,n,-npw,-nph)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(-npw,-nph)).rgb;
       gi+=  giFF(ddiff5,n,0,nph)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(0,nph)).rgb;
       gi+=  giFF(ddiff6,n,0,-nph)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(0,-nph)).rgb;
       gi+=  giFF(ddiff7,n,npw,0)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(npw,0)).rgb;
       gi+=  giFF(ddiff8,n,-npw,0)*texture2D(gdiffuse, gl_TexCoord[0].st+vec2(-npw,0)).rgb;*/

       //increase sampling area:
       pw += incx;  
       ph += incy;    
    } 
    ao/=24.0;
    //gi/=24.0;


    //gl_FragColor = vec4((col.rgb-vec3(ao)*ssaoStrength*10+gi*5)*ambient,1.0);
    gl_FragColor = vec4((col-vec3(ao)*ssaoStrength*5)*ambient,1);
    //gl_FragColor = vec4(vec3(1-ao*20),1);
}