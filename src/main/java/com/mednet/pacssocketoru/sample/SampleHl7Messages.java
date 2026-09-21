package com.mednet.pacssocketoru.sample;

/**
 * Sample inbound PACS ORU^R01 (HL7 v2.3) as received from RIS — segments concatenated.
 */
public final class SampleHl7Messages {

    private SampleHl7Messages() {
    }

    public static final String ORU_R01_V23_CONCATENATED =
            "MSH|^~\\&|RIS|RIS|HIS|HIS|20260916172733.5623||ORU^R01|2609161727332417|P|2.3||||||||||PacsReport"
                    + "PID|1|SR1067552|SR1067552||DUMMY^DUMMY||19930507|M|||^^Faridabad^Haryana^121009^India||8860153609|8860153609||"
                    + "PV1|1|OP|||366686:71374||||||||||||||W26-27-1|0Year||||||||||||||||||||||||"
                    + "ORC|NW|O:366686:1|O:366686:1||||20260916181000||20260916181000|||REFERRING^ NDMC  DISPENSARY|||||SHB"
                    + "OBR|1|O:366686:1|O:366686:1|Ul1510^Ultrasound Neck|||20260916181000||||||||&Ultrasound Neck|"
                    + "REFERRING^AYUSH^SINGHAL^^^DR.||MEDNET|Ul1510|SHB|SHB_US|||US|F||20260916181000|||||||||20260916181000"
                    + "OBX|1|TX|Ultrasound Neck^Ultrasound Neck||Ultrasound Neck\\X0D\\\\X0A\\«Transcribed By Name» "
                    + "Bilateral upper limb arterial and venous doppler for AV fistula planning: \\X0D\\\\X0A\\"
                    + "Right upper limb: \\X0D\\\\X0A\\At wrist: \\X0D\\\\X0A\\"
                    + "Radial artery:                           mm, ** cm/sec\\X0D\\\\X0A\\"
                    + "Cephalic vein:                          mm, compressible.\\X0D\\\\X0A\\"
                    + "At elbow: \\X0D\\\\X0A\\"
                    + "Brachial artery:                        mm, ** cm/sec\\X0D\\\\X0A\\"
                    + "Cephalic vein:                          mm, compressible.\\X0D\\\\X0A\\"
                    + "Basilic vein:                             mm, compressible.\\X0D\\\\X0A\\ \\X0D\\\\X0A\\"
                    + "Left upper limb: \\X0D\\\\X0A\\At wrist: \\X0D\\\\X0A\\"
                    + "Radial artery:                           mm, ** cm/sec\\X0D\\\\X0A\\"
                    + "Cephalic vein:                          mm, compressible.\\X0D\\\\X0A\\ \\X0D\\\\X0A\\"
                    + "At elbow: \\X0D\\\\X0A\\"
                    + "Brachial artery:                        mm, ** cm/sec\\X0D\\\\X0A\\"
                    + "Cephalic vein:                          mm, compressible.\\X0D\\\\X0A\\"
                    + "Basilic vein:                             mm, compressible. \\X0D\\\\X0A\\"
                    + "No e/o deep vein thrombosis in bilateral axillary, brachial veins. \\X0D\\\\X0A\\"
                    + "Adv- Clinical correlation.||||||F|20260916171723^20260916171723||20260916172726||"
                    + "S22243^Dr. Apoorv Goel|||20260916171723";
}
