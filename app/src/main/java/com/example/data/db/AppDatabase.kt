package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.NoteEntity
import com.example.util.HashUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(entities = [NoteEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN category TEXT DEFAULT NULL")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_category` ON notes (`category`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isDeleted` ON notes (`isDeleted`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glass_notes_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialNotes(database.noteDao())
                    }
                }
            }
        }

        suspend fun populateInitialNotes(dao: NoteDao) {
            val now = System.currentTimeMillis()

            val photosynthesisHtml = """<div style="font-family:-apple-system,BlinkMacSystemFont,Roboto,sans-serif;max-width:600px;margin:0 auto;color:#1C1E21;line-height:1.7;">
  <h1 style="font-size:26px;font-weight:800;letter-spacing:-0.4px;margin-bottom:8px;color:#1C1E21">Photosynthesis 🌿</h1>
  <p style="color:#70757A;font-size:14px;margin-top:0">Biology • Chapter 4 • Plant Physiology</p>

  <div style="background:#E8F7EC;border:1px solid #C4EDCF;border-radius:18px;padding:16px 18px;margin:16px 0;">
    <h3 style="margin:0 0 6px 0;color:#2EB85C;font-size:16px">Key Equation</h3>
    <code style="font-size:15px;font-weight:700;color:#1C1E21">6CO₂ + 6H₂O + Sunlight ➔ C₆H₁₂O₆ + 6O₂</code>
  </div>

  <h2 style="font-size:19px;font-weight:700;margin-top:20px;color:#1C1E21">1. Introduction</h2>
  <p style="color:#33383F;font-size:15px">Photosynthesis is the fundamental biological process by which green plants, algae, and certain cyanobacteria synthesize organic food molecules using sunlight, atmospheric carbon dioxide (CO₂), and water (H₂O), releasing oxygen (O₂) as a vital byproduct.</p>

  <h2 style="font-size:19px;font-weight:700;margin-top:20px;color:#1C1E21">2. Two Stages of Photosynthesis</h2>
  <ul style="padding-left:20px;color:#33383F;font-size:15px">
    <li><b>Light-Dependent Reactions:</b> Occur in the thylakoid membranes of chloroplasts. Photons excite chlorophyll pigments, splitting H₂O (photolysis) to produce ATP, NADPH, and free O₂.</li>
    <li><b>Light-Independent Reactions (Calvin Cycle):</b> Occur in the stroma. The enzyme RuBisCO fixes CO₂ into 3-PGA, subsequently reduced to glucose using energy from ATP and NADPH.</li>
  </ul>

  <h2 style="font-size:19px;font-weight:700;margin-top:20px;color:#1C1E21">3. Factors Affecting Rate</h2>
  <p style="color:#33383F;font-size:15px">According to <i>Blackman's Law of Limiting Factors</i>, the photosynthetic rate is governed by light intensity, temperature (optimum 25°C–35°C), and atmospheric CO₂ concentration.</p>
</div>"""

            val humanReproductionContent = """# Human Reproduction
Biology • Chapter 3 • Human Embryology & Reproductive Biology

## 1. Introduction
Human reproduction is the biological process by which new human beings are produced through sexual reproduction involving male and female gametes (sperm and ovum).

## 2. Male Reproductive System
- **Testes**: Primary male sex organs located inside the scrotum for thermoregulation (2-2.5°C lower than internal body temp).
- **Seminiferous Tubules**: Functional units where spermatogenesis occurs under the influence of FSH and testosterone.
- **Leydig Cells**: Interstitial cells synthesizing and secreting androgens (testosterone).
- **Sertoli Cells**: Provide nourishment and structural support to developing sperm cells.

## 3. Female Reproductive System
- **Ovaries**: Produce the female gamete (ovum) and steroid hormones (estrogen, progesterone).
- **Fallopian Tubes (Oviducts)**:
  - *Infundibulum* with fimbriae to collect the released ovum.
  - *Ampulla*: The site where fertilization takes place.
  - *Isthmus*: Narrow passage leading to the uterus.
- **Uterus**: Composed of Perimetrium (outer serosa), Myometrium (smooth muscle layer), and Endometrium (glandular inner lining shedding during menstruation).

## 4. Gametogenesis & Fertilization
- **Spermatogenesis**: Continuous production of four functional haploid spermatozoa from one spermatogonium.
- **Oogenesis**: Discontinuous formation of one functional haploid ovum and polar bodies.
- **Acrosome Reaction**: Hyaluronidase and acrosin enzymes penetrate the zona pellucida of the ovum.

## 5. Embryonic Development & Implantation
1. Zygote undergoes cleavage divisions forming Morula (16-cell stage).
2. Morula transforms into Blastocyst with outer Trophoblast and Inner Cell Mass (ICM).
3. Blastocyst embeds in the uterine endometrium (Implantation) ~7 days post-fertilization."""

            val plantKingdomHtml = """<div style="font-family:-apple-system,sans-serif;max-width:600px;margin:0 auto;color:#1C1E21;line-height:1.7;">
  <h1 style="font-size:26px;font-weight:800;color:#1C1E21">Plant Kingdom 🌿</h1>
  <p style="color:#70757A;font-size:14px">Biology • Taxonomy & Morphology</p>
  <h2 style="font-size:18px;font-weight:700">1. Algae (Thallophytes)</h2>
  <p>Simple, autotrophic, mostly aquatic organisms. Major classes include Chlorophyceae (green algae), Phaeophyceae (brown algae), and Rhodophyceae (red algae).</p>
  <h2 style="font-size:18px;font-weight:700">2. Bryophytes (Amphibians of Plant Kingdom)</h2>
  <p>Non-vascular terrestrial plants dependent on water for sexual reproduction. Include liverworts and mosses with dominant gametophyte generation.</p>
  <h2 style="font-size:18px;font-weight:700">3. Pteridophytes</h2>
  <p>First vascular terrestrial plants possessing xylem and phloem. Sporophyte is dominant, producing spores in sporangia (e.g., ferns, horsetails).</p>
</div>"""

            val chemicalBondingContent = """# Chemical Bonding & Molecular Structure
Chemistry • Physical & Inorganic Chemistry

## 1. Ionic Bonding (Electrovalent)
Formed by complete electrostatic transfer of one or more electrons from a metal (low ionization enthalpy) to a non-metal (high electron gain enthalpy). Lattice energy is the principal stabilizing factor.

## 2. Covalent Bonding & Octet Rule
Formed by sharing electron pairs between atoms. 
- Lewis dot structures describe valence electron distribution.
- Formal charge = Valence e⁻ - Nonbonding e⁻ - 0.5*(Bonding e⁻)

## 3. VSEPR Theory
Valence Shell Electron Pair Repulsion theory predicts geometry:
- 2 pairs: Linear (180°)
- 3 pairs: Trigonal planar (120°)
- 4 pairs: Tetrahedral (109.5°)
- 5 pairs: Trigonal bipyramidal (90°, 120°)
- 6 pairs: Octahedral (90°)

## 4. Hybridization
Intermixing of atomic orbitals of slightly differing energies:
- **sp³**: Methane (CH₄), Tetrahedral, 109.5°
- **sp²**: Ethene (C₂H₄), Trigonal planar, 120°
- **sp**: Ethyne (C₂H₂), Linear, 180°"""

            val ecologyHtml = """<div style="font-family:-apple-system,sans-serif;max-width:600px;margin:0 auto;color:#1C1E21;line-height:1.7;">
  <h1 style="font-size:26px;font-weight:800;color:#1C1E21">Ecology & Ecosystem Dynamics 🌍</h1>
  <p style="color:#70757A;font-size:14px">Biology • Environmental Science</p>
  <h2 style="font-size:18px;font-weight:700">1. Trophic Levels & Energy Flow</h2>
  <p>Energy enters ecosystems via solar radiation captured by primary producers. According to Lindeman's 10% Law, only ~10% of energy is transferred to the next trophic level.</p>
  <h2 style="font-size:18px;font-weight:700">2. Biogeochemical Cycles</h2>
  <p>Carbon and nitrogen cycles ensure continuous nutrient recycling between abiotic reservoirs and living organisms.</p>
</div>"""

            val kinematicsPdf = """# Kinematics & Laws of Motion
Physics • Mechanics

## 1. Equations of Rectilinear Motion
For constant acceleration *a*:
- v = u + at
- s = ut + 0.5 * a * t²
- v² = u² + 2as
- sₙ = u + a/2 * (2n - 1)

## 2. Projectile Motion
- Time of flight: T = (2 u sin θ) / g
- Maximum height: H = (u² sin² θ) / (2g)
- Horizontal range: R = (u² sin 2θ) / g (Maximum at θ = 45°)

## 3. Newton's Three Laws
1. **First Law**: Inertia of rest, motion, and direction.
2. **Second Law**: F = dp/dt = m * a
3. **Third Law**: For every action, there is an equal and opposite reaction."""

            val notes = listOf(
                // 1. Photosynthesis (HTML)
                NoteEntity(
                    id = "note-bio-photosynthesis",
                    title = "Photosynthesis 🌿",
                    type = "html",
                    content = photosynthesisHtml,
                    pinned = true,
                    category = "Biology",
                    createdAt = now - 7_200_000,
                    updatedAt = now - 7_200_000,
                    hash = HashUtil.noteHash("Photosynthesis 🌿", photosynthesisHtml)
                ),
                // 2. Human Reproduction (PDF)
                NoteEntity(
                    id = "note-bio-human-rep",
                    title = "Human Reproduction",
                    type = "pdf",
                    content = humanReproductionContent,
                    pinned = true,
                    category = "Biology",
                    createdAt = now - 14_400_000,
                    updatedAt = now - 14_400_000,
                    hash = HashUtil.noteHash("Human Reproduction", humanReproductionContent)
                ),
                // 3. Plant Kingdom (HTML)
                NoteEntity(
                    id = "note-bio-plant-kingdom",
                    title = "Plant Kingdom",
                    type = "html",
                    content = plantKingdomHtml,
                    pinned = false,
                    category = "Biology",
                    createdAt = now - 86_400_000,
                    updatedAt = now - 86_400_000,
                    hash = HashUtil.noteHash("Plant Kingdom", plantKingdomHtml)
                ),
                // 4. Chemical Bonding (PDF)
                NoteEntity(
                    id = "note-chem-bonding",
                    title = "Chemical Bonding",
                    type = "pdf",
                    content = chemicalBondingContent,
                    pinned = true,
                    category = "Chemistry",
                    createdAt = now - 172_800_000,
                    updatedAt = now - 172_800_000,
                    hash = HashUtil.noteHash("Chemical Bonding", chemicalBondingContent)
                ),
                // 5. Ecology Notes (HTML)
                NoteEntity(
                    id = "note-bio-ecology",
                    title = "Ecology Notes",
                    type = "html",
                    content = ecologyHtml,
                    pinned = false,
                    category = "Biology",
                    createdAt = now - 259_200_000,
                    updatedAt = now - 259_200_000,
                    hash = HashUtil.noteHash("Ecology Notes", ecologyHtml)
                ),
                // 6. Kinematics (PDF)
                NoteEntity(
                    id = "note-phys-kinematics",
                    title = "Kinematics & Dynamics",
                    type = "pdf",
                    content = kinematicsPdf,
                    pinned = true,
                    category = "Physics",
                    createdAt = now - 345_600_000,
                    updatedAt = now - 345_600_000,
                    hash = HashUtil.noteHash("Kinematics & Dynamics", kinematicsPdf)
                ),
                // 7. Reproduction in Plants (HTML)
                NoteEntity(
                    id = "note-bio-rep-plants",
                    title = "Reproduction in Plants",
                    type = "html",
                    content = "<p>Comprehensive guide to angiosperm reproduction, microsporogenesis, megasporogenesis, and double fertilization.</p>",
                    pinned = false,
                    category = "Biology",
                    createdAt = now - 400_000_000,
                    updatedAt = now - 400_000_000,
                    hash = HashUtil.noteHash("Reproduction in Plants", "Reproduction in Plants")
                ),
                // 8. Reproductive Health (PDF)
                NoteEntity(
                    id = "note-bio-rep-health",
                    title = "Reproductive Health",
                    type = "pdf",
                    content = "# Reproductive Health\nWorld Health Organization guidelines, contraception methods, and adolescent health.",
                    pinned = false,
                    category = "Biology",
                    createdAt = now - 450_000_000,
                    updatedAt = now - 450_000_000,
                    hash = HashUtil.noteHash("Reproductive Health", "Reproductive Health")
                ),
                // 9. Asexual Reproduction (HTML)
                NoteEntity(
                    id = "note-bio-asexual",
                    title = "Asexual Reproduction",
                    type = "html",
                    content = "<p>Modes of asexual reproduction in organisms: binary fission, spore formation, vegetative propagation, regeneration.</p>",
                    pinned = false,
                    category = "Biology",
                    createdAt = now - 500_000_000,
                    updatedAt = now - 500_000_000,
                    hash = HashUtil.noteHash("Asexual Reproduction", "Asexual Reproduction")
                ),
                // Physics Folder
                NoteEntity(id = "phys-1", title = "Electromagnetism & Waves", type = "html", content = "<p>Maxwell's equations and electromagnetic spectrum waves.</p>", category = "Physics", pinned = false),
                NoteEntity(id = "phys-2", title = "Optics & Light", type = "pdf", content = "# Optics & Light\nRefraction, Snell's law, and wave diffraction.", category = "Physics", pinned = false),
                NoteEntity(id = "phys-3", title = "Thermodynamics in Physics", type = "html", content = "<p>First and second laws of thermodynamics, Carnot cycle.</p>", category = "Physics", pinned = false),
                NoteEntity(id = "phys-4", title = "Newton's Laws of Motion", type = "html", content = "<p>Action, reaction, inertia and momentum.</p>", category = "Physics", pinned = false),
                NoteEntity(id = "phys-5", title = "Work, Energy & Power", type = "pdf", content = "# Work, Energy & Power\nConservative vs non-conservative forces.", category = "Physics", pinned = false),
                NoteEntity(id = "phys-6", title = "Gravitation & Kepler Laws", type = "html", content = "<p>Universal law of gravitation and planetary orbits.</p>", category = "Physics", pinned = false),
                NoteEntity(id = "phys-7", title = "Quantum Mechanics Intro", type = "pdf", content = "# Quantum Mechanics\nWave-particle duality and Schrödinger equation.", category = "Physics", pinned = false),

                // Chemistry Folder
                NoteEntity(id = "chem-1", title = "Organic Chemistry Basics", type = "html", content = "<p>IUPAC nomenclature, isomerism, and reaction mechanisms.</p>", category = "Chemistry", pinned = false),
                NoteEntity(id = "chem-2", title = "Periodic Trends & Elements", type = "pdf", content = "# Periodic Trends\nElectronegativity, electron affinity, and atomic radius.", category = "Chemistry", pinned = false),
                NoteEntity(id = "chem-3", title = "Acids, Bases & Salts", type = "html", content = "<p>pH calculations, buffer solutions, and acid-base titrations.</p>", category = "Chemistry", pinned = false),
                NoteEntity(id = "chem-4", title = "Electrochemistry & Redox", type = "pdf", content = "# Electrochemistry\nGalvanic cells and standard electrode potentials.", category = "Chemistry", pinned = false),
                NoteEntity(id = "chem-5", title = "Chemical Kinetics", type = "html", content = "<p>Rate equations, activation energy, and Arrhenius equation.</p>", category = "Chemistry", pinned = false),

                // Mathematics Folder
                NoteEntity(id = "math-1", title = "Calculus & Derivatives", type = "pdf", content = "# Calculus & Derivatives\nLimits, continuity, chain rule, and optimization.", category = "Mathematics", pinned = false),
                NoteEntity(id = "math-2", title = "Linear Algebra & Matrices", type = "html", content = "<p>Matrix operations, determinants, eigenvalues, and vectors.</p>", category = "Mathematics", pinned = false),
                NoteEntity(id = "math-3", title = "Trigonometric Identities", type = "html", content = "<p>Angle sum formulas, unit circle values, and proofs.</p>", category = "Mathematics", pinned = false),
                NoteEntity(id = "math-4", title = "Probability & Statistics", type = "pdf", content = "# Probability & Statistics\nBayes' theorem, normal distribution, variance.", category = "Mathematics", pinned = false),

                // English Folder
                NoteEntity(id = "eng-1", title = "Essay Writing Techniques", type = "html", content = "<p>Thesis development, paragraph unity, transition words, and conclusion styling.</p>", category = "English", pinned = false),
                NoteEntity(id = "eng-2", title = "Grammar & Composition", type = "html", content = "<p>Advanced sentence structures, active voice, and syntax mastery.</p>", category = "English", pinned = false),
                NoteEntity(id = "eng-3", title = "Literary Devices & Analysis", type = "pdf", content = "# Literary Devices\nMetaphors, allegories, irony, and rhetorical questions.", category = "English", pinned = false),

                // My Notes Folder
                NoteEntity(id = "mynote-1", title = "Daily Study Schedule", type = "text", content = "# Daily Study Schedule\n- 08:00 AM: Biology Photosynthesis revision\n- 11:00 AM: Chemistry Chemical Bonding\n- 03:00 PM: Physics Kinematics problem sets\n- 07:00 PM: Math Calculus exercises", category = "My Notes", pinned = true),
                NoteEntity(id = "mynote-2", title = "Exam Quick Checklist", type = "text", content = "[x] Biology flashcards reviewed\n[x] Physics formula sheet memorized\n[ ] Chemistry reaction mechanisms\n[ ] Math past papers", category = "My Notes", pinned = false),
                NoteEntity(id = "mynote-3", title = "Formula Cheat Sheet", type = "text", content = "Physics & Chemistry key formulas summary for quick revision.", category = "My Notes", pinned = false),
                NoteEntity(id = "mynote-4", title = "Weekly Study Goals", type = "text", content = "Target: Complete 4 chapter summaries and 2 mock test papers.", category = "My Notes", pinned = false),
                NoteEntity(id = "mynote-5", title = "Quick Summary Template", type = "text", content = "Topic:\nKey Concepts:\nFormulae / Terms:\nQuestions:", category = "My Notes", pinned = false)
            )

            dao.insertNotes(notes)
        }
    }
}
