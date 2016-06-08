/*
 * Copyright (c) 2016, Regents of the University of California and
 * contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.calnet.groovy.transform;

import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.Arrays;

public class HashCodeSalts {
    public static void main(String[] args) {
        printSalts(System.out, (args.length > 0 ? Integer.parseInt(args[0]) : 128));
    }

    public static void printSalts(PrintStream ps, int quantity) {
        ps.println("    public static int[] salts = {");
        final int[] result = generateSalts(quantity);
        for (int i = 0; i < result.length; i++) {
            ps.printf("        %-14s %s\n", Integer.toString(result[i]) + (i + 1 < result.length ? "," : ""), "// " + Integer.toString(i));
        }
        ps.println("    };");
    }

    public static int[] generateSalts(int quantity) {
        SecureRandom rand = new SecureRandom();

        int[] result = new int[quantity];
        for (int i = 0; i < quantity; i++) {
            // unlikely, but nevertheless, generate again if the
            // random number generator returned a 0.
            while (result[i] == 0) {
                result[i] = rand.nextInt();
            }
        }

        return result;
    }

    /**
     * Ensures that at least "quantity" salts are available.
     *
     * @return true if the salt array was grown or false if it wasn't because the quantity is already available.
     */
    public static boolean ensureMaxSalts(int quantity) {
        if (salts.length < quantity) {
            int originalLength = salts.length;
            int growBy = quantity - originalLength;

            // generate additional salts
            int[] additionalSalts = generateSalts(growBy);

            // replace the salts array with a copy of the old array expanded to the new capacity
            salts = Arrays.copyOf(salts, quantity);

            // fill the new slots with additional salts
            System.arraycopy(additionalSalts, 0, salts, originalLength, growBy);

            return true;
        } else {
            return false;
        }

    }

    public static int[] salts = {
            285764410,     // 0
            1906742706,    // 1
            1525674003,    // 2
            1098895243,    // 3
            -335099420,    // 4
            1145916158,    // 5
            1010250057,    // 6
            2087933071,    // 7
            402004515,     // 8
            -402608705,    // 9
            478183203,     // 10
            646929603,     // 11
            1531111157,    // 12
            281494709,     // 13
            -417274963,    // 14
            294034749,     // 15
            -2029078419,   // 16
            1781925153,    // 17
            -1158729740,   // 18
            -1989573390,   // 19
            -1091460230,   // 20
            44432891,      // 21
            -2136385591,   // 22
            990935505,     // 23
            -313127163,    // 24
            1708864012,    // 25
            -1918724383,   // 26
            -1763263846,   // 27
            1544091447,    // 28
            -1843333475,   // 29
            2008259052,    // 30
            364370456,     // 31
            877556536,     // 32
            477914614,     // 33
            1918880569,    // 34
            -1323687890,   // 35
            249537155,     // 36
            -304453056,    // 37
            -130205461,    // 38
            -1930286127,   // 39
            2107984542,    // 40
            -1575986250,   // 41
            -430080262,    // 42
            1160273216,    // 43
            -758664550,    // 44
            2108417414,    // 45
            1106588529,    // 46
            -1258267823,   // 47
            -2102142812,   // 48
            1728061261,    // 49
            567372551,     // 50
            -922349511,    // 51
            1180556596,    // 52
            -1614758562,   // 53
            1220469881,    // 54
            -306169349,    // 55
            -1384294510,   // 56
            -571333537,    // 57
            -328630955,    // 58
            735815560,     // 59
            1753726528,    // 60
            1254801806,    // 61
            -1781563972,   // 62
            673548259,     // 63
            840792338,     // 64
            1641610080,    // 65
            1466099087,    // 66
            1222243458,    // 67
            -713726741,    // 68
            1106899032,    // 69
            183589866,     // 70
            -134031789,    // 71
            -1825060586,   // 72
            1819414009,    // 73
            -1759132424,   // 74
            1545608255,    // 75
            927490656,     // 76
            206176119,     // 77
            62108340,      // 78
            1338079443,    // 79
            776681571,     // 80
            1048396800,    // 81
            -894481318,    // 82
            -1384316024,   // 83
            1765813254,    // 84
            -1204111243,   // 85
            -1898529665,   // 86
            -1781149172,   // 87
            1293436897,    // 88
            -1381992324,   // 89
            -839851918,    // 90
            257078975,     // 91
            1985033077,    // 92
            -645059904,    // 93
            -1319783713,   // 94
            289089577,     // 95
            -835174709,    // 96
            549260310,     // 97
            1236246228,    // 98
            1637981507,    // 99
            -584472249,    // 100
            1828673520,    // 101
            845533995,     // 102
            -186308031,    // 103
            1663538219,    // 104
            -457528160,    // 105
            398048405,     // 106
            -688279366,    // 107
            1980223391,    // 108
            1720798815,    // 109
            -88341734,     // 110
            904379588,     // 111
            462168917,     // 112
            326231790,     // 113
            2055987869,    // 114
            1389771430,    // 115
            -1135454972,   // 116
            -321709827,    // 117
            394507745,     // 118
            -525971756,    // 119
            1404839413,    // 120
            1071480134,    // 121
            -239207660,    // 122
            -1883068385,   // 123
            1777311594,    // 124
            1720292680,    // 125
            -1462384520,   // 126
            -1558997280    // 127
    };
}
