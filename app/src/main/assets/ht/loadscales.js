var s = document.getElementById('iadscaleid');
if (!s) return false;
var n = 3;
while (n > 0) {
    const o = document.createElement('option');
    const v = n * window.manifest?.defaultImage?.height;
    o.value = 'height=' + v;
    o.innerText = 'height: ' + v;
    if (n == 3) o.selected = true;
    s.appendChild(o);
    n--;
}
var t = document.getElementById('iadtasksid');
if (!t) return false;
var m = availableProcessors;
const m1 = m;
while (m >= 1) {
    const o = document.createElement('option');
    o.value = m;
    o.innerText = m.toString();
    if (m == m1) o.selected = true;
    t.appendChild(o);
    m = Math.floor(m/2);
}
return true;
