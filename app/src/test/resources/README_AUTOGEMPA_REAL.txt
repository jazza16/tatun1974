Tempat penyimpanan file respons asli BMKG autogempa.json:
Simpan file JSON asli dari https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json ke path:
app/src/test/resources/autogempa_real.json

Catatan:
- File `app/src/test/resources/autogempa_synthetic.json` adalah data sintetis untuk pengujian struktur & regresi unit test.
- Apabila `app/src/test/resources/autogempa_real.json` tersedia dan berisi JSON respons asli BMKG, unit test `SirenDeduplicationAndValidationTest` akan otomatis mem-parse dan mengujinya.
