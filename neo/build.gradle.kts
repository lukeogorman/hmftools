
plugins {
    id("com.hartwig.java-conventions")
    application
}

description = "HMF Tools - Neoepitopes"

dependencies {
    implementation(project(":hmf-common"))
    implementation(project(":patient-db"))
    testImplementation(libs.junit)
    annotationProcessor(libs.immutables.value)
    compileOnly(libs.immutables.value)
}

application {
    mainClass.set("com.hartwig.hmftools.neo.epitope.NeoEpitopeAnnotator")
}
