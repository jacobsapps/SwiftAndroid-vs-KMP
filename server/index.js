const express = require("express");
const { categories } = require("./coasters.json");

const app = express();
const port = process.env.PORT || 3000;

const rollerCoasters = Array.isArray(categories) ? categories : [];

app.get("/roller-coasters", (_, res) => {
  res.json({ categories: rollerCoasters });
});

app.get("/roller-coasters/search", (req, res) => {
  const query = String(req.query.name ?? "").trim().toLowerCase();
  if (!query) {
    res.json({ categories: [] });
    return;
  }

  const matches = rollerCoasters.filter((item) =>
    item.name.toLowerCase().includes(query)
  );

  res.json({ categories: matches });
});

app.listen(port, () => {
  console.log(`Coaster API listening on http://localhost:${port}`);
});
