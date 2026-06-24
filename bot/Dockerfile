FROM python:3.9-slim

WORKDIR /app

COPY bot/requirements.txt .
RUN pip install -r requirements.txt

COPY bot/bot.py .

CMD ["python", "bot.py"]