package nz.govt.companiesoffice.register.migration

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.security.MessageDigest

/**
 * Helper class to generate checksums for migration files.
 * Run this to get the correct checksums for your approval tests.
 *
 * Usage:
 * 1. Run this class as a main method
 * 2. Copy the generated checksums into your MigrationApprovalTest
 * 3. Never run this again unless you're intentionally updating a migration
 */
object MigrationChecksumGenerator {

    private val migrationPath = "db/migration"
    private val resourceResolver = PathMatchingResourcePatternResolver()

    @JvmStatic
    fun main(args: Array<String>) {
        println("🔍 Generating checksums for migration files...")
        println("=" * 60)

        try {
            val migrationFiles = getAllMigrationFiles().sorted()

            if (migrationFiles.isEmpty()) {
                println("⚠️  No migration files found!")
                return
            }

            println("Found ${migrationFiles.size} migration files:")
            println()

            migrationFiles.forEach { migrationFile ->
                val checksum = calculateMigrationChecksum(migrationFile)
                val content = getMigrationContent(migrationFile)
                val lineCount = content.lines().size

                println("📄 Migration: $migrationFile")
                println("   Checksum: $checksum")
                println("   Lines: $lineCount")
                println("   Size: ${content.length} characters")
                println()
            }

            println("=" * 60)
            println("📝 Copy these checksums into your MigrationApprovalTest:")
            println()

            migrationFiles.forEach { migrationFile ->
                val checksum = calculateMigrationChecksum(migrationFile)
                val testMethodName = migrationFile.replace(".", "_").replace("-", "_")

                println("// Expected checksum for $migrationFile")
                println("val expectedChecksum = \"$checksum\"")
                println()
            }

            println("=" * 60)
            println("⚠️  IMPORTANT REMINDERS:")
            println("- These checksums should NEVER change once deployed")
            println("- If you need to modify a migration, create a NEW one instead")
            println("- Only update checksums during development before first deployment")
            println("- Always review migration changes with the team")
            println("=" * 60)
        } catch (e: Exception) {
            println("❌ Error generating checksums: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun calculateMigrationChecksum(migrationFile: String): String {
        val content = getMigrationContent(migrationFile)
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun getMigrationContent(migrationFile: String): String {
        return try {
            val resource = ClassPathResource("$migrationPath/$migrationFile")
            if (resource.exists()) {
                resource.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw IllegalArgumentException("Migration file not found: $migrationFile")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to read migration file $migrationFile: ${e.message}", e)
        }
    }

    private fun getAllMigrationFiles(): Set<String> {
        return try {
            val resources = resourceResolver.getResources("classpath:$migrationPath/V*.sql")
            resources.map { resource ->
                resource.filename ?: throw RuntimeException("Unable to get filename for resource: $resource")
            }.toSet()
        } catch (e: Exception) {
            throw RuntimeException("Failed to list migration files: ${e.message}", e)
        }
    }
}
