from flask import Flask, request, jsonify
from flask_cors import CORS
import mysql.connector

app = Flask(__name__)
CORS(app)

# 🔗 MySQL adatbázis kapcsolat
db_config = {
    "host": "localhost",
    "user": "root",
    "password": "KrisztiaN12",
    "database": "trinexon"
}

# 🤖 AI logika – ajánlott fizetés kiszámítása
def recommend_salary(department):
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("""
            SELECT amount FROM finance_records
            JOIN employees ON finance_records.employee_id = employees.id
            WHERE finance_records.type = 'salary'
              AND TRIM(LOWER(employees.department)) = TRIM(LOWER(%s))
        """, (department,))
        salaries = [row[0] for row in cursor.fetchall()]
        cursor.close()
        conn.close()

        print("📊 Lekérdezett fizetések:", salaries)

        if not salaries:
            return None

        # 🔁 Decimal → float konverzió
        float_salaries = [float(s) for s in salaries]

        avg_salary = sum(float_salaries) / len(float_salaries)
        return round(avg_salary * 1.05, 2)

    except Exception as e:
        print("❌ AI hiba:", e)
        return None
# 🌐 Kezdőoldal
@app.route('/')
def home():
    return '✔️ AI fizetés API működik!'

# 📋 Dolgozók lekérdezése (hire_date ISO formátumban!)
@app.route('/api/employees', methods=['GET'])
def get_employees():
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM employees")
        rows = cursor.fetchall()
        cursor.close()
        conn.close()

        # 🛠️ hire_date ISO-formátumra konvertálás
        for row in rows:
            if isinstance(row.get('hire_date'), (str, type(None))):
                continue
            row['hire_date'] = row['hire_date'].strftime('%Y-%m-%d')

        return jsonify(rows)
    except Exception as e:
        print("❌ Hiba dolgozók lekérdezésekor:", e)
        return jsonify({"error": str(e)}), 500

# 💾 Fizetés mentése
@app.route('/api/salary', methods=['POST'])
def save_salary():
    data = request.get_json()
    employee_id = data.get("employee_id")
    salary = data.get("salary")
    month = data.get("month")

    if not all([employee_id, salary, month]):
        return jsonify({"error": "Hiányzó mezők"}), 400

    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # 🛡️ Ellenőrzés: dolgozó létezik-e
        cursor.execute("SELECT COUNT(*) FROM employees WHERE id = %s", (employee_id,))
        if cursor.fetchone()[0] == 0:
            cursor.close()
            conn.close()
            return jsonify({"error": "Nem létező dolgozó ID"}), 404

        # 💾 Mentés
        cursor.execute("""
            INSERT INTO finance_records (employee_id, amount, month, type)
            VALUES (%s, %s, %s, 'salary')
        """, (employee_id, salary, month))
        conn.commit()
        cursor.close()
        conn.close()
        return jsonify({"status": "Sikeres mentés"}), 200

    except Exception as e:
        print("❌ Hiba fizetés mentésekor:", e)
        return jsonify({"error": str(e)}), 500

# 🔍 AI fizetés ajánlás
@app.route('/api/salary/recommend', methods=['GET'])
def salary_recommendation():
    department = request.args.get("department")
    if not department:
        return jsonify({"error": "Hiányzó osztály"}), 400

    recommendation = recommend_salary(department)
    if recommendation is None:
        return jsonify({"message": "Nincs elérhető adat az osztályhoz"}), 404

    return jsonify({
        "recommended_salary": recommendation,
        "note": "AI javaslat 5% emeléssel"
    })

# 🚀 Indítás
if __name__ == '__main__':
    app.run(debug=True)