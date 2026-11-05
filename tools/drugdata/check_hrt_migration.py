"""Execute the additive HRT migrations against Room v18 with preservation sentinels."""
import json
import re
import sqlite3
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
schema = json.loads((ROOT / "app/schemas/com.isaakhanimann.journal.data.room.AppDatabase/18.json").read_text(encoding="utf-8"))["database"]
source = (ROOT / "app/src/main/java/com/isaakhanimann/journal/data/room/AppDatabase.kt").read_text(encoding="utf-8")
conn = sqlite3.connect(":memory:")
conn.execute("PRAGMA foreign_keys=ON")
for entity in schema["entities"]:
    conn.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
    for index in entity.get("indices", []):
        conn.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
# Preserve literal names, unusual units and consumer data without normalizing them.
conn.execute("INSERT INTO Ingestion (id,substanceName,time,creationDate,administrationRoute,dose,isDoseAnEstimate,units,experienceId,consumerName) VALUES(42,'Estradiol',123456789,123456789,'ORAL',2.5,0,'tablet',17,'person B')")
before = conn.execute("SELECT * FROM Ingestion").fetchall()
for start in (18, 19, 20):
    block = source.split(f"val MIGRATION_{start}_{start+1} =", 1)[1].split("\n        val MIGRATION_", 1)[0]
    statements = re.findall(r'db.execSQL\("([^"\n]+)"\)', block)
    assert statements, start
    for statement in statements:
        conn.execute(statement)
assert conn.execute("SELECT * FROM Ingestion").fetchall() == before
conn.execute("INSERT INTO medication_links VALUES(42,'test-id','identified','Estradiol','automatic')")
conn.execute("DELETE FROM Ingestion WHERE id=42")
assert conn.execute("SELECT count(*) FROM medication_links").fetchone()[0] == 0
target = sqlite3.connect(":memory:")
latest = json.loads((ROOT / "app/schemas/com.isaakhanimann.journal.data.room.AppDatabase/21.json").read_text(encoding="utf-8"))["database"]
for entity in latest["entities"]:
    table = entity["tableName"]
    target.execute(entity["createSql"].replace("${TABLE_NAME}", table))
    actual = conn.execute(f"PRAGMA table_info(`{table}`)").fetchall()
    expected = target.execute(f"PRAGMA table_info(`{table}`)").fetchall()
    assert sorted(actual, key=lambda row: row[1]) == sorted(expected, key=lambda row: row[1]), table
print("HRT v18 -> v21 SQL preservation, linkage cascade and Room v21 column schemas passed")
