// Based on the JS implementation from
// Stripe and Kevin Hufnagl
//
// Vertex shader code extracted with Gemini 2.5 Pro
//
// https://gist.github.com/jordienr/64bcf75f8b08641f205bd6a1a0d4ce1d
// https://stripe.com
// https://kevinhufnagl.com

attribute vec4 aPosition;
attribute vec2 aUv;
attribute vec2 aUvNorm;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform float uTime;
uniform vec2 uResolution;

// Gradient Colors
uniform vec3 uBaseColor;
uniform vec3 uColor1;
uniform vec3 uColor2;
uniform vec3 uColor3;

uniform float uNoiseSeeds[3];
uniform float uNoiseSpeed;

varying vec3 vColor;
varying float vUvY;

// Simplex noise
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

float snoise(vec3 v) {
    const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i  = floor(v + dot(v, C.yyy) );
    vec3 x0 =   v - i + dot(i, C.xxx) ;
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min( g.xyz, l.zxy );
    vec3 i2 = max( g.xyz, l.zxy );
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;
    i = mod289(i);
    vec4 p = permute( permute( permute(
    i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
    + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
    + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));
    float n_ = 0.142857142857;
    vec3  ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_ );
    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4( x.xy, y.xy );
    vec4 b1 = vec4( x.zw, y.zw );
    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;
    vec3 p0 = vec3(a0.xy,h.x);
    vec3 p1 = vec3(a0.zw,h.y);
    vec3 p2 = vec3(a1.xy,h.z);
    vec3 p3 = vec3(a1.zw,h.w);
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;

    return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3) ) );
}

vec3 blendNormal(vec3 base, vec3 blend) {
    return blend;
}

vec3 blendNormal(vec3 base, vec3 blend, float opacity) {
    return (blendNormal(base, blend) * opacity + base * (1.0 - opacity));
}

void main() {
    // Global noise config
    float globalNoiseSpeed = 5e-6;
    float time = uTime * globalNoiseSpeed;

    vec2 noiseCoord = aUvNorm * vec2(uResolution.x / uResolution.y, 1.0) * 2.0;

    // Deform Params
    float noiseAmp = 0.9;
    vec2 deformFreq = vec2(3.0, 4.0);
    float deformFlow = 3.0;
    float deformSpeed = 200.0;
    float deformSeed = uNoiseSeeds[0];

    // 1. Calculate Base Noise for Vertex Displacement
    float noise = snoise(vec3(
        noiseCoord.x * deformFreq.x + time * deformFlow,
        noiseCoord.y * deformFreq.y,
        time * deformSpeed + deformSeed
    ) * 0.05) * noiseAmp;

    // Smoothen the peaks
    noise = noise * 0.5 + 0.5;
    noise = smoothstep(0.5, 0.8, noise);

    // Fade noise at edges
    noise *= 1.0 - pow(abs(aUvNorm.y), 2.0);
    noise = max(0.0, noise);

    // Apply Displacement
    vec3 pos = vec3(aPosition.x, aPosition.y + noise, aPosition.z);

    // 2. Calculate Colors (Wave Layers)
    vColor = uBaseColor;

    vec3 waveColors[3];
    waveColors[0] = uColor1;
    waveColors[1] = uColor2;
    waveColors[2] = uColor3;

    for (int i = 0; i < 3; i++) {
        float iFloat = float(i);

        // Per-layer noise params
        vec2 layerFreq = vec2(2.0 + iFloat / 3.0, 3.0 + iFloat / 3.0);
        float layerSpeed = 11.0 + 0.3 * iFloat;
        float layerFlow = 6.5 + 0.3 * iFloat;
        float layerSeed = uNoiseSeeds[i]; // seed + 10 * i in JS
        float layerFloor = 0.1;
        float layerCeil = 0.63 + 0.07 * iFloat;

        float noiseVal = smoothstep(
            layerFloor,
            layerCeil,
            snoise(vec3(
                noiseCoord.x * layerFreq.x + time * layerFlow,
                noiseCoord.y * layerFreq.y,
                time * layerSpeed + layerSeed
            ) * 0.07) / 2.0 + 0.5
        );

        vColor = blendNormal(vColor, waveColors[i], pow(noiseVal, 4.0));
    }

    vUvY = aUvNorm.y * 0.5 + 0.5;
    gl_Position = uProjectionMatrix * uModelViewMatrix * vec4(pos, 1.0);
}