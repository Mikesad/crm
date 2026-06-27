package com.crm.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

/**
 * BCrypt 密码哈希生成器
 *
 * <p>用法：</p>
 * <pre>
 *   mvn -pl crm-tools exec:java -Dexec.args="123456"
 *   # 或多个密码：
 *   mvn -pl crm-tools exec:java -Dexec.args="123456 admin888 demo123"
 * </pre>
 *
 * <p>输出每行一个 hash，可直接复制到 SQL 的 password 字段。</p>
 *
 * <p>强度默认 10，与后端 {@code BCryptPasswordEncoder()} 默认一致。
 * 每次运行 salt 都不同，所以同一明文每次输出不同 hash，但都能通过
 * {@code BCryptPasswordEncoder.matches(rawPassword, hash)} 验证。</p>
 */
public class GeneratePasswordHash {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: GeneratePasswordHash <password> [password...]");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  GeneratePasswordHash 123456");
            System.out.println("  GeneratePasswordHash 123456 admin888 demo123");
            System.exit(1);
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Arrays.stream(args).forEach(raw -> {
            String hash = encoder.encode(raw);
            boolean matches = encoder.matches(raw, hash);
            System.out.println(hash);
            // 自检：再次匹配必须成功
            if (!matches) {
                throw new IllegalStateException("Hash self-check failed for: " + raw);
            }
        });
    }
}