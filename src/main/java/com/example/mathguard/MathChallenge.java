package com.example.mathguard;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

enum Op { ADD, SUB, MUL }

public class MathChallenge {
    private final int a, b, result;
    private final Op op;

    public MathChallenge(int a, int b, Op op) {
        this.a = a;
        this.b = b;
        this.op = op;
        this.result = switch (op) {
            case ADD -> a + b;
            case SUB -> a - b;
            case MUL -> a * b;
        };
    }

    public int getResult() { return result; }

    public String display() {
        String sym = switch (op) { case ADD -> "+"; case SUB -> "−"; case MUL -> "×"; };
        return a + " " + sym + " " + b + " = ?";
    }

    /** Generator mit konfigurierbaren Modi */
    public static MathChallenge random(Random r, FileConfiguration cfg) {
        List<Supplier<MathChallenge>> pool = new ArrayList<>();
        int maxR = cfg.getInt("max-result", 99);

        if (cfg.getBoolean("modes.addition", true)) {
            pool.add(() -> {
                int a = r.nextInt(0, 100), b = r.nextInt(0, 100);
                if (a + b > maxR) return null;
                return new MathChallenge(a, b, Op.ADD);
            });
        }
        if (cfg.getBoolean("modes.subtraction_full", true)) {
            pool.add(() -> {
                int a = r.nextInt(0, 100), b = r.nextInt(0, 100);
                if (a < b) { int t = a; a = b; b = t; }
                int res = a - b;
                if (res < 0 || res > maxR) return null;
                return new MathChallenge(a, b, Op.SUB);
            });
        }
        if (cfg.getBoolean("modes.subtraction_simple_ones", true)) {
            pool.add(() -> {
                int a = r.nextInt(0, 100), b = r.nextInt(1, 10); // einstelliger Subtrahend
                if (a < b) { int t = a; a = b; b = t; }
                int res = a - b;
                if (res < 0 || res > maxR) return null;
                return new MathChallenge(a, b, Op.SUB);
            });
        }
        if (cfg.getBoolean("modes.subtraction_mid_10_20", true)) {
            pool.add(() -> {
                int b = r.nextInt(10, 21);  // 10..20
                int a = r.nextInt(b, 100);  // a >= b
                int res = a - b;
                if (res < 0 || res > maxR) return null;
                return new MathChallenge(a, b, Op.SUB);
            });
        }
        if (cfg.getBoolean("modes.multiplication_small_table", true)) {
            pool.add(() -> {
                int a = r.nextInt(1, 10), b = r.nextInt(1, 10); // 1..9
                if (a * b > maxR) return null;
                return new MathChallenge(a, b, Op.MUL);
            });
        }
        if (cfg.getBoolean("modes.multiplication_double_only", true)) {
            pool.add(() -> {
                int a = r.nextInt(1, 50), b = 2;
                if (a * b > maxR) return null;
                return new MathChallenge(a, b, Op.MUL);
            });
        }

        if (pool.isEmpty()) {
            int a = r.nextInt(0, 50), b = r.nextInt(0, 50);
            return new MathChallenge(a, b, Op.ADD);
        }

        for (int tries = 0; tries < 200; tries++) {
            MathChallenge mc = pool.get(r.nextInt(pool.size())).get();
            if (mc != null) return mc;
        }
        int a = r.nextInt(0, 50), b = r.nextInt(0, 50);
        return new MathChallenge(a, b, Op.ADD);
    }
}
