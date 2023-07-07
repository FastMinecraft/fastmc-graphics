layout(std140) uniform Camera {
    mat4 projection;
    mat4 modelView;
    mat4 combined;
    mat4 invProjection;
    mat4 invModelView;
    mat4 invCombined;
    vec2 screenSize;
    float partialTicks;
};