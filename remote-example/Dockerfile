FROM python:3.11-slim-buster

RUN mkdir -p /app
WORKDIR /app

COPY requirements.txt /app/
RUN pip install -r requirements.txt

COPY logging.ini /app/
COPY shopping_pb2.py /app/
COPY shopping_types.py /app/
COPY main.py /app/

EXPOSE 8000
CMD ["gunicorn", "-b", "0.0.0.0:8000", "-w 4", "--log-config", "logging.ini", "main:app"]
