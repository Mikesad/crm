package com.crm.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

/**
 * 验证 BCrypt hash 与明文是否匹配（用于自检 crm_full.sql 中的种子 hash）
 *
 * <p>用法：</p>
 * <pre>
 *   mvn -pl crm-tools exec:java -Dexec.mainClass=com.crm.tools.VerifyPasswordHash \
 *       -Dexec.args="\$2a\$10\$... 123456"
 *   # 或：
 *   mvn -pl crm-tools exec:java -Dexec.mainClass=com.crm.tools.VerifyPasswordHash \
 *       -Dexec.args="--all"
 *   # 自检 crm_full.sql 中所有种子 hash
 * </pre>
 */
public class VerifyPasswordHash {

    /** crm_full.sql 中嵌入的种子 hash（密码统一 123456） */
    private static final String SEED_HASH = "$2a$10$ZtTWH2VV9JXLAv/lCDnMLO5Jqxe5ZN6JLibkW3cAuzU0w7gdYM0nC";

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (args.length == 1 && "--all".equals(args[0])) {
            // 自检模式：验证种子 hash 与 123456 匹配
            boolean ok = encoder.matches("123456", SEED_HASH);
            System.out.println("seed hash matches \"123456\": " + (ok ? "OK" : "FAIL"));
            if (!ok) System.exit(1);
            return;
        }

        if (args.length != 2) {
            System.out.println("Usage:");
            System.out.println("  VerifyPasswordHash <hash> <plaintext>");
            System.out.println("  VerifyPasswordHash --all");
            System.exit(1);
        }

        String hash = args[0];
        String raw  = args[1];
        boolean ok = encoder.matches(raw, hash);
        System.out.println("hash=" + hash);
        System.out.println("plaintext=" + raw);
        System.out.println("result: " + (ok ? "OK" : "FAIL"));
        System.exit(ok ? 0 : 1);
    }
}