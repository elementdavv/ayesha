var s = document.getElementById('iadscaleid');
if (!s) return false;
var n = 3;
while (n > 0) {
    const o = document.createElement('option');
    const v = n * window.manifest?.defaultImage?.height;
    o.value = 'height=' + v;
    o.innerText = 'high: ' + v;
    if (n == 3) o.selected = true;
    s.appendChild(o);
    n--;
}
return true;
