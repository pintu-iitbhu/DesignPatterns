package com.rcpky;

import com.rcpky.command.CommandRegistry;
import com.rcpky.core.Repository;
import com.rcpky.core.WorkingDirectory;

public class GitVCSApp {

    public static void main(String[] args) {
        Repository repo = new Repository("Dev User <dev@example.com>");
        CommandRegistry registry = new CommandRegistry();

        // ── Scenario 1: Init ──────────────────────────────────────────────────
        section("1. Initialize Repository");
        registry.execute(repo, new String[]{"init"});

        // ── Scenario 2: First Commit ──────────────────────────────────────────
        section("2. Create files and make first commit");
        WorkingDirectory wd = repo.getWorkingDirectory();
        wd.addFile("README.md", "# My Project\nWelcome to Git VCS.\n");
        wd.addFile("Main.java", "public class Main {\n    public static void main(String[] a) {}\n}\n");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Initial commit"});

        // ── Scenario 3: Status after clean commit ─────────────────────────────
        section("3. Status (clean tree)");
        registry.execute(repo, new String[]{"status"});

        // ── Scenario 4: Modify file, check diff ──────────────────────────────
        section("4. Modify README and check diff (working dir vs HEAD)");
        wd.addFile("README.md", "# My Project\nWelcome to Git VCS.\nAdded new line.\n");
        registry.execute(repo, new String[]{"diff", "HEAD"});

        // ── Scenario 5: Stage partial, show status, commit ───────────────────
        section("5. Stage README only, show status");
        registry.execute(repo, new String[]{"add", "README\\.md"});
        registry.execute(repo, new String[]{"status"});

        section("5b. Commit staged README");
        registry.execute(repo, new String[]{"commit", "-m", "Update README with new line"});

        // ── Scenario 6: diff --cached (staged vs HEAD) ────────────────────────
        section("6. Untracked file + diff --cached");
        wd.addFile("Utils.java", "public class Utils {}\n");
        registry.execute(repo, new String[]{"add", "Utils\\.java"});
        registry.execute(repo, new String[]{"diff", "--cached"});

        // ── Scenario 7: Branch – create and switch ────────────────────────────
        section("7. Create 'feature' branch and commit on it");
        registry.execute(repo, new String[]{"commit", "-m", "Add Utils class"});
        registry.execute(repo, new String[]{"checkout", "-b", "feature"});
        wd.addFile("Feature.java", "public class Feature { /* new feature */ }\n");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Add Feature class"});

        // ── Scenario 8: Log on feature branch ────────────────────────────────
        section("8. Log on 'feature' branch");
        registry.execute(repo, new String[]{"log"});

        // ── Scenario 9: Switch back to main, verify isolation ─────────────────
        section("9. Switch back to 'main' – Feature.java should not exist");
        registry.execute(repo, new String[]{"checkout", "main"});
        registry.execute(repo, new String[]{"status"});
        System.out.println("  Feature.java in working dir: " + wd.hasFile("Feature.java"));

        // ── Scenario 10: -am commit (stage-and-commit in one step) ───────────
        section("10. -am commit: modify Main.java, commit without explicit add");
        wd.addFile("Main.java", "public class Main {\n    public static void main(String[] a) {\n        System.out.println(\"Hello\");\n    }\n}\n");
        registry.execute(repo, new String[]{"commit", "-am", "Implement Main with Hello"});
        registry.execute(repo, new String[]{"log"});

        // ── Scenario 11: Reset HEAD~1 ─────────────────────────────────────────
        section("11. Reset HEAD~1 on main");
        registry.execute(repo, new String[]{"reset", "HEAD~1"});
        registry.execute(repo, new String[]{"log"});

        // ── Scenario 12: Help ─────────────────────────────────────────────────
        section("12. Help");
        registry.execute(repo, new String[]{"help"});
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("══════════════════════════════════════════════════════");
    }
}
