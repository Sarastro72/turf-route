let myMap
let canvas
let mappa
let refreshCb
const options = {
    lat: 59.34,
    lng: 17.90,
    zoom: 14,
    style: "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
}

function initZoneMap(callback) {
    mappa = new Mappa('Leaflet')
    refreshCb = callback
}

function clearZoneMap() {
    clear();
}

function getMapBounds() {
    let b = myMap.map.getBounds()
    return {
        sw: {lat: b.getSouthWest().lat, long: b.getSouthWest().lng},
        ne: {lat: b.getNorthEast().lat, long: b.getNorthEast().lng}
    }
}

function drawZone(zone) {
    fill(255, 130, 100);
    stroke(0, 0, 0, 255)
    strokeWeight(1)
    const zPos = myMap.latLngToPixel(zone.lat, zone.long);
    ellipse(zPos.x, zPos.y, 9, 9);
}

function drawRoutes(zone) {
    if (zone.routes) {
        const zPos = myMap.latLngToPixel(zone.lat, zone.long);
        zone.routes.forEach(r => {
                const toPos = myMap.latLngToPixel(r.toLat, r.toLong);
                const off = delta(zPos, toPos, 5)
                const norm = normal(zPos, toPos, 10)
                setStyleFromWeight(r.weight)
                bezier(
                    zPos.x,
                    zPos.y,
                    zPos.x + off.x + norm.x,
                    zPos.y + off.y + norm.y,
                    toPos.x - off.x + norm.x,
                    toPos.y - off.y + norm.y,
                    toPos.x,
                    toPos.y
                )
            }
        )
    }
}

const alphas = [32, 32, 64, 128, 255, 255, 255, 255, 255, 255, 255]
const strokeWeights = [0.2, 0.3, 0.4, 0.6, 0.8, 1, 1.5, 2, 2.5, 3, 3.5]

function setStyleFromWeight(w) {
    const alpha = 255 // alphas[w]
    const weight = strokeWeights[w] + 0.2
    noFill()
    stroke(0, 0, 0, alpha)
    strokeWeight(weight)
}

function delta(from, to, frac = 1) {
    return {
        x: int((to.x - from.x) / frac),
        y: int((to.y - from.y) / frac)
    }
}

function normal(from, to, frac = 1) {
    return {
        x: int((from.y - to.y) / frac),
        y: int((to.x - from.x) / frac)
    }
}

function setup() {
    canvas = createCanvas(1024, 800);
    myMap = mappa.tileMap(options);
    myMap.overlay(canvas)

    myMap.onChange(reDraw);
}

function draw() {
    // Run on every frame
}

function reDraw() {
    clearZoneMap()
    refreshCb(getMapBounds())
}
