dependencies {
    modCore("dev.fastmc:fastmc-common:1.1-SNAPSHOT") {
        isTransitive = false
    }

    modCore("dev.luna5ama:kmogus-core:1.0-SNAPSHOT")
    modCore("dev.luna5ama:kmogus-joml:1.0-SNAPSHOT")
    modCore("dev.luna5ama:kmogus-struct-api:1.0-SNAPSHOT")
    modCore(project(":shared:structs"))

    modCore("dev.luna5ama:gl-wrapper-core:1.0-SNAPSHOT")
}