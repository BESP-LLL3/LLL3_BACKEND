from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import numpy as np

app = Flask(__name__)

model = SentenceTransformer("BM-K/KoSimCSE-roberta-multitask")

def cosine_similarity(vec1, vec2):
    vec1 = np.array(vec1)
    vec2 = np.array(vec2)
    return float(np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2)))

@app.route("/embed", methods=["POST"])
def embed():
    # JSON으로 받은 데이터에서 "keyword" 값을 추출
    data = request.get_json()

    # 키워드가 없는 경우 처리
    keyword = data.get("keyword", "")

    if not keyword:
        return jsonify({"error": "임베드를 생성할 키워드가 필요합니다."}), 400

    # 임베딩
    keyword_embedding = model.encode([keyword], convert_to_numpy=True)[0]

    # 결과 반환
    return jsonify({"embedding": keyword_embedding.tolist()})

@app.route("/embed/batch", methods=["POST"])
def batchEmbed():
    data = request.get_json()
    keywords = data.get("keywords", [])

    if not keywords:
        return jsonify({"error": "임베드를 생성할 키워드 리스트가 필요합니다."}), 400

    embeddings = model.encode(keywords, convert_to_numpy=True)
    return jsonify({"embeddings": [e.tolist() for e in embeddings]})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5050)