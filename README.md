# Aesteria

## Maritime Oil Spill Detection, Drift Analysis and AIS Attribution

Aesteria is a Smart India Hackathon prototype for analysing suspected marine oil-spill events by combining spill detection, drift/origin estimation and AIS vessel attribution.

The prototype pipeline is:

Spill Detection
→ Drift / Origin Estimation
→ AIS Search
→ Distance + Time Correlation
→ Ranked Vessel Candidates
→ Evidence Dashboard

> **Prototype Mode:** This demonstration uses synthetic AIS records and simulated spill/drift metadata. The attribution pipeline is designed to accept real AIS and environmental observations when available.

---

## Project Structure

```text
Aesteria/
├── model/
│   └── notebook.ipynb
│
├── backend/
│   ├── src/
│   │   ├── ais/
│   │   │   ├── AISAttribution.java
│   │   │   ├── SpillEvent.java
│   │   │   ├── VesselRecord.java
│   │   │   └── VesselResult.java
│   │   │
│   │   ├── server/
│   │   │   └── ApiServer.java
│   │   │
│   ├── data/
│   │   ├── drift_summary.csv
│   │   └── results.json
│   │
│   ├── lib/
│   │   └── postgresql-42.7.13.jar
│   │
│   └── bin/
│
├── frontend/
│   └── index.html
│
├── Dockerfile
└── README.md