SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS patient;
CREATE TABLE patient
(   id int NOT NULL AUTO_INCREMENT,
    cpctId varchar(50),
    registrationDate DATE,
    gender varchar(10),
    hospital varchar(255),
    birthYear int,
    cancerType varchar(255),
    cancerSubtype varchar(255),
    deathDate DATE,
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS sample;
CREATE TABLE sample
(   sampleId varchar(255) NOT NULL,
    patientId int NOT NULL,
    arrivalDate DATE NOT NULL,
    samplingDate DATE,
    tumorPercentage DOUBLE PRECISION,
    PRIMARY KEY (sampleId),
    FOREIGN KEY (patientId) REFERENCES patient(id)
);

DROP TABLE IF EXISTS biopsy;
CREATE TABLE biopsy
(   id int NOT NULL,
    sampleId varchar(255),
    patientId int NOT NULL,
    biopsySite varchar(255),
    biopsyLocation varchar(255),
    biopsyDate DATE,
    PRIMARY KEY (id),
    FOREIGN KEY (sampleId) REFERENCES sample(sampleId),
    FOREIGN KEY (patientId) REFERENCES patient(id)
);

DROP TABLE IF EXISTS treatment;
CREATE TABLE treatment
(   id int NOT NULL,
    biopsyId int,
    patientId int NOT NULL,
    treatmentGiven varchar(3),
    startDate DATE,
    endDate DATE,
    name varchar(255),
    type varchar(255),
    PRIMARY KEY (id),
    FOREIGN KEY (biopsyId) REFERENCES biopsy(id),
    FOREIGN KEY (patientId) REFERENCES patient(id)
);

DROP TABLE IF EXISTS drug;
CREATE TABLE drug
(   id int NOT NULL AUTO_INCREMENT,
    treatmentId int,
    patientId int NOT NULL,
    startDate DATE,
    endDate DATE,
    name varchar(255),
    type varchar(255),
    PRIMARY KEY (id),
    FOREIGN KEY (treatmentId) REFERENCES treatment(id),
    FOREIGN KEY (patientId) REFERENCES patient(id)
);

DROP TABLE IF EXISTS treatmentResponse;
CREATE TABLE treatmentResponse
(   id int NOT NULL AUTO_INCREMENT,
    treatmentId int,
    patientId int NOT NULL,
    measurementDone varchar(5),
    responseDate DATE,
    response varchar(25),
    PRIMARY KEY (id),
    FOREIGN KEY (treatmentId) REFERENCES treatment(id),
    FOREIGN KEY (patientId) REFERENCES patient(id)
);

DROP TABLE IF EXISTS comprehensiveSomaticVariant;
DROP TABLE IF EXISTS somaticVariant;
CREATE TABLE somaticVariant
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    chromosome varchar(255) NOT NULL,
    position int not null,
    filter varchar(255) NOT NULL,
    ref varchar(255) NOT NULL,
    alt varchar(255) NOT NULL,
    gene varchar(255) NOT NULL,
    cosmicId varchar(255) NOT NULL,
    dbsnpId varchar(255) NOT NULL,
    effect varchar(255) NOT NULL,
    microhomology varchar(255) NOT NULL,
    repeatSequence varchar(255) NOT NULL,
    repeatCount int NOT NULL,
    refGenomeContext varchar(255) NOT NULL,
    alleleReadCount int NOT NULL,
    totalReadCount int NOT NULL,
    adjustedVaf DOUBLE PRECISION NOT NULL,
    adjustedCopyNumber DOUBLE PRECISION NOT NULL,
    highConfidence BOOLEAN NOT NULL,
    trinucleotideContext varchar(3) NOT NULL,
    clonality varchar(455) NOT NULL,
    loh BOOLEAN NOT NULL,
    mappability DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (id),
    INDEX(sampleId),
    INDEX(filter),
    INDEX(gene)
);

DROP TABLE IF EXISTS copyNumber;
CREATE TABLE copyNumber
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    chromosome varchar(255) NOT NULL,
    start int not null,
    end int not null,
    segmentStartSupport varchar(255) NOT NULL,
    segmentEndSupport varchar(255) NOT NULL,
    bafCount int not null,
    observedBaf DOUBLE PRECISION not null,
    actualBaf DOUBLE PRECISION not null,
    copyNumber DOUBLE PRECISION not null,
    copyNumberMethod varchar(255) NOT NULL,
    PRIMARY KEY (id),
    INDEX(sampleId)
);

DROP TABLE IF EXISTS copyNumberGermline;
CREATE TABLE copyNumberGermline
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    chromosome varchar(255) NOT NULL,
    start int not null,
    end int not null,
    segmentStartSupport varchar(255) NOT NULL,
    segmentEndSupport varchar(255) NOT NULL,
    bafCount int not null,
    observedBaf DOUBLE PRECISION not null,
    actualBaf DOUBLE PRECISION not null,
    copyNumber DOUBLE PRECISION not null,
    copyNumberMethod varchar(255) NOT NULL,
    PRIMARY KEY (id),
    INDEX(sampleId)
);

DROP TABLE IF EXISTS purityRange;
CREATE TABLE purityRange
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    purity DOUBLE PRECISION not null,
    normFactor DOUBLE PRECISION not null,
    score DOUBLE PRECISION not null,
    ploidy DOUBLE PRECISION not null,
    diploidProportion DOUBLE PRECISION not null,
    PRIMARY KEY (id),
    INDEX(sampleId)
);

DROP TABLE IF EXISTS purityScore;
DROP VIEW IF EXISTS purity;
DROP TABLE IF EXISTS purity;
CREATE TABLE purity
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    version VARCHAR(255) NOT NULL,
    sampleId varchar(255) NOT NULL,
    gender varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    qcStatus varchar(255) NOT NULL,
    purity DOUBLE PRECISION not null,
    normFactor DOUBLE PRECISION not null,
    score DOUBLE PRECISION not null,
    ploidy DOUBLE PRECISION not null,
    diploidProportion DOUBLE PRECISION not null,
    polyclonalProportion DOUBLE PRECISION not null,
    minPurity DOUBLE PRECISION not null,
    maxPurity DOUBLE PRECISION not null,
    minPloidy DOUBLE PRECISION not null,
    maxPloidy DOUBLE PRECISION not null,
    minDiploidProportion DOUBLE PRECISION not null,
    maxDiploidProportion DOUBLE PRECISION not null,
    PRIMARY KEY (id),
    INDEX(sampleId)
);


DROP TABLE IF EXISTS copyNumberRegion;
CREATE TABLE copyNumberRegion
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    chromosome varchar(255) NOT NULL,
    start int not null,
    end int not null,
    germlineStatus varchar(255) NOT NULL,
    svCluster BOOLEAN NOT NULL,
    ratioSupport BOOLEAN NOT NULL,
    segmentStartSupport varchar(255) NOT NULL,
    bafCount int not null,
    observedBaf DOUBLE PRECISION not null,
    observedTumorRatio DOUBLE PRECISION not null,
    observedNormalRatio DOUBLE PRECISION not null,
    observedTumorRatioCount int not null,
    gcContent DOUBLE PRECISION not null,
    modelPloidy int not null,
    modelBaf DOUBLE PRECISION not null,
    modelTumorRatio DOUBLE PRECISION not null,
    actualTumorBaf DOUBLE PRECISION not null,
    actualTumorCopyNumber DOUBLE PRECISION not null,
    refNormalisedTumorCopyNumber DOUBLE PRECISION not null,
    cnvDeviation DOUBLE PRECISION not null,
    bafDeviation DOUBLE PRECISION not null,
    totalDeviation DOUBLE PRECISION not null,
    fittedBaf DOUBLE PRECISION not null,
    fittedCopyNumber DOUBLE PRECISION not null,
    PRIMARY KEY (id),
    INDEX(sampleId)
);

DROP TABLE IF EXISTS structuralVariant;
CREATE TABLE structuralVariant
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    startChromosome varchar(255) NOT NULL,
    endChromosome varchar(255) NOT NULL,
    startPosition int not null,
    endPosition int not null,
    startOrientation tinyint not null,
    endOrientation tinyint not null,
    startHomologySequence varchar(255) not null,
    endHomologySequence varchar(255) not null,
    startAF DOUBLE PRECISION,
    endAF DOUBLE PRECISION,
    ploidy DOUBLE PRECISION,
    adjustedStartAF DOUBLE PRECISION,
    adjustedEndAF DOUBLE PRECISION,
    adjustedStartCopyNumber DOUBLE PRECISION,
    adjustedEndCopyNumber DOUBLE PRECISION,
    adjustedStartCopyNumberChange DOUBLE PRECISION,
    adjustedEndCopyNumberChange DOUBLE PRECISION,
    insertSequence varchar(255) not null,
    type varchar(255) NOT NULL,
    PRIMARY KEY (id),
    INDEX(sampleId)
);

DROP TABLE IF EXISTS geneCopyNumber;
CREATE TABLE geneCopyNumber
(   id int NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    chromosome varchar(255) NOT NULL,
    start int not null,
    end int not null,
    gene varchar(255) NOT NULL,
    chromosomeBand varchar(255) NOT NULL,
    transcriptId varchar(255) NOT NULL,
    transcriptVersion int not null,
    minCopyNumber DOUBLE PRECISION not null,
    maxCopyNumber DOUBLE PRECISION not null,
    meanCopyNumber DOUBLE PRECISION not null,
    somaticRegions int not null,
    germlineHomRegions int not null,
    germlineHetRegions int not null,
    PRIMARY KEY (id),
    INDEX(sampleId),
    INDEX(gene)
);

DROP TABLE IF EXISTS clinicalFindings;
CREATE TABLE clinicalFindings
(   id int NOT NULL AUTO_INCREMENT,
    level varchar(30),
    patientId varchar(20),
    ecrfItem varchar(100),
    formStatus varchar(30),
    formLocked varchar(5),
    message varchar(1000),
    details varchar(1000),
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS ecrf;
CREATE TABLE ecrf
(   id int NOT NULL AUTO_INCREMENT,
    patientId varchar(20),
    studyEvent varchar(100),
    studyEventKey int not null,
    form varchar(100),
    formKey int not null,
    itemGroup varchar(100),
    itemGroupKey int not null,
    item varchar(100),
    itemValue varchar(1500),
    status varchar(30),
    locked varchar(5),
    sequenced varchar(5),
    fieldName varchar(100),
    relevant varchar(5),
    PRIMARY KEY (id),
    INDEX(patientId),
    INDEX(studyEvent),
    INDEX(form),
    INDEX(itemGroup),
    INDEX(item),
    INDEX(itemValue),
    INDEX(status),
    INDEX(locked),
    INDEX(sequenced),
    INDEX(fieldName),
    INDEX(relevant)
);

DROP TABLE IF EXISTS ecrfDatamodel;
CREATE TABLE ecrfDatamodel
(   fieldName varchar(100),
    description varchar(500),
    codeList varchar(3000),
    relevant varchar(5)
);


DROP TABLE IF EXISTS formsMetadata;
CREATE TABLE formsMetadata
(   id int NOT NULL,
    tableName varchar(20),
    form varchar(20),
    status varchar(30),
    locked varchar(5),
    UNIQUE KEY (id, tableName, form)
);

DROP TABLE IF EXISTS drupEcrf;
CREATE TABLE drupEcrf
(   id int NOT NULL AUTO_INCREMENT,
    patientId varchar(20),
    studyEvent varchar(100),
    studyEventKey int not null,
    form varchar(100),
    formKey int not null,
    itemGroup varchar(100),
    itemGroupKey int not null,
    item varchar(100),
    itemValue varchar(15000),
    status varchar(30),
    locked varchar(5),
    sequenced varchar(5),
    fieldName varchar(100),
    relevant varchar(5),
    PRIMARY KEY (id),
    INDEX(patientId),
    INDEX(studyEvent),
    INDEX(form),
    INDEX(itemGroup),
    INDEX(item),
    INDEX(status),
    INDEX(sequenced),
    INDEX(fieldName),
    INDEX(relevant)
);

DROP TABLE IF EXISTS drupEcrfDatamodel;
CREATE TABLE drupEcrfDatamodel
(   fieldName varchar(100),
    description varchar(500),
    codeList varchar(5000),
    relevant varchar(5)
);

DROP TABLE IF EXISTS structuralVariantDisruption;
DROP TABLE IF EXISTS structuralVariantFusion;
DROP TABLE IF EXISTS structuralVariantBreakend;

CREATE TABLE structuralVariantBreakend
(   id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    structuralVariantId INT NOT NULL,
    isStartEnd BOOLEAN NOT NULL,
    gene VARCHAR(512) NOT NULL, # length here comes from ensembl db schema
    geneId VARCHAR(128) NOT NULL, # length here comes from ensembl db schema
    transcriptId VARCHAR(128) NOT NULL, # length here comes from ensembl db schema
    isCanonicalTranscript BOOLEAN NOT NULL,
    strand TINYINT NOT NULL,
    exonRankUpstream TINYINT UNSIGNED,
    exonRankDownstream TINYINT UNSIGNED,
    exonPhaseUpstream TINYINT,
    exonPhaseDownstream TINYINT,
    exonMax TINYINT NOT NULL,
    PRIMARY KEY (id),
    INDEX(structuralVariantId),
    INDEX(gene),
    INDEX(geneId),
    INDEX(transcriptId)
);

CREATE TABLE structuralVariantDisruption
(
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    breakendId INT UNSIGNED NOT NULL,
    isReported BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    INDEX(breakendId)
);

CREATE TABLE structuralVariantFusion
(
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    fivePrimeBreakendId INT UNSIGNED NOT NULL,
    threePrimeBreakendId INT UNSIGNED NOT NULL,
    isReported BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    INDEX(fivePrimeBreakendId),
    INDEX(threePrimeBreakendId)
);

SET FOREIGN_KEY_CHECKS = 1;
