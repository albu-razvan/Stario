// Based on the JS implementation from
// Stripe and Kevin Hufnagl
//
// Fragment shader code extracted with Gemini 2.5 Pro
//
// https://gist.github.com/jordienr/64bcf75f8b08641f205bd6a1a0d4ce1d
// https://stripe.com
// https://kevinhufnagl.com

precision mediump float;

varying vec3 vColor;
varying float vUvY;

void main() {
    gl_FragColor = vec4(vColor, 1.0 - vUvY);
}