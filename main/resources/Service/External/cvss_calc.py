import sys
from cvss import CVSS3, CVSS4

def calculate_cvss_score(vector: str):
    try:
        if vector.startswith("CVSS:3.1/"):
            cvss = CVSS3(vector)
        elif vector.startswith("CVSS:4.0/"):
            cvss = CVSS4(vector)
        else:
            print("Unsupported CVSS version format", file=sys.stderr)
            return
        base_score = cvss.scores()[0]  # [base, temporal, environmental]
        print(base_score)
    except Exception as e:
        print("Error:", str(e), file=sys.stderr)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python cvss_calc.py <CVSS_vector>")
    else:
        calculate_cvss_score(sys.argv[1])
