package com.hartwig.hmftools.common.lims;

import com.hartwig.hmftools.common.lims.cohort.LimsCohortConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LimsChecker {

    private static final Logger LOGGER = LogManager.getLogger(LimsChecker.class);

    private LimsChecker() {
    }

    public static boolean checkViralInsertions(@Nullable LimsJsonSampleData sampleData, @Nullable LimsCohortConfig cohort,
            @NotNull String sampleId) {
        if (sampleData != null && cohort != null) {
            if (sampleData.reportViralPresence()) {
                if (!cohort.reportViral()) {
                    LOGGER.warn("Consent of viral insertions is true, but must be false for sample '{}'", sampleId);
                }
                return true;
            } else {
                if (cohort.reportViral()) {
                    LOGGER.warn("Consent of viral insertions is false, but must be true for sample '{}'", sampleId);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean checkReportPgx(@Nullable LimsJsonSampleData sampleData, @Nullable LimsCohortConfig cohort,
            @NotNull String sampleId) {
        if (sampleData != null && cohort != null) {
            if (sampleData.reportPgx()) {
                if (!cohort.reportPeach()) {
                    LOGGER.warn("Consent of pharmogenetics is true, but must be false for sample '{}'", sampleId);
                }
                return true;
            } else {
                if (cohort.reportPeach()) {
                    LOGGER.warn("Consent of pharmogenetics is false, but must be true for sample '{}'", sampleId);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean checkGermlineVariants(@Nullable LimsJsonSampleData sampleData, @Nullable LimsCohortConfig cohort,
            @NotNull String sampleId) {
        if (sampleData != null && cohort != null) {
            if (sampleData.reportGermlineVariants()) {
                if (!cohort.reportGermline()) {
                    LOGGER.warn("Consent of report germline variants is true, but must be false for sample '{}'", sampleId);
                }
                return true;
            } else {
                if (cohort.reportGermline()) {
                    LOGGER.warn("Consent of report germline variants is false, but must be true for sample '{}'", sampleId);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    @Nullable
    public static String toHospitalPathologySampleIdForReport(@NotNull String hospitalPathologySampleId, @NotNull String tumorSampleId,
            @NotNull LimsCohortConfig cohortConfig) {
        if (cohortConfig.requireHospitalPAId()) {
            if (!hospitalPathologySampleId.equals(Lims.NOT_AVAILABLE_STRING) && !hospitalPathologySampleId.isEmpty()) {
                return hospitalPathologySampleId;
            } else {

                LOGGER.warn("Missing or invalid hospital pathology sample ID for sample '{}': {}. Please fix!",
                        tumorSampleId,
                        hospitalPathologySampleId);

                return null;
            }
        } else {
            if (!hospitalPathologySampleId.isEmpty() && !hospitalPathologySampleId.equals(Lims.NOT_AVAILABLE_STRING)) {
                LOGGER.info("Skipping hospital pathology sample ID for sample '{}': {}", hospitalPathologySampleId, tumorSampleId);
            }

            return null;
        }
    }

    public static void checkHospitalPatientId(@NotNull String hospitalPatientId, @NotNull String sampleId,
            @NotNull LimsCohortConfig cohortConfig) {
        if (cohortConfig.requireHospitalId()) {
            if (hospitalPatientId.equals(Lims.NOT_AVAILABLE_STRING) || hospitalPatientId.isEmpty()) {
                LOGGER.warn("Missing hospital patient sample ID for sample '{}': {}. Please fix!", sampleId, hospitalPatientId);
            }
        }
    }
}
