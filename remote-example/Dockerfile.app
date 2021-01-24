FROM python:3.7-slim-buster

ARG STATEFUN_WHEEL_FILE=apache_flink_statefun-2.2_SNAPSHOT-py3-none-any.whl

RUN mkdir -p /app
WORKDIR /app

COPY $STATEFUN_WHEEL_FILE /app/
RUN pip install $STATEFUN_WHEEL_FILE

COPY requirements.txt /app/
RUN pip install -r requirements.txt

COPY logging.ini /app/
COPY shopping_pb2.py /app/
COPY main.py /app/

EXPOSE 8000
CMD ["gunicorn", "-b", "0.0.0.0:8000", "-w 4", "--log-config", "logging.ini", "main:app"]
