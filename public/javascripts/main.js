console.log("aaa")
document.addEventListener('DOMContentLoaded', () => {
	Array.prototype.forEach.call(
	document.querySelectorAll(".entry input"), (a) => {
	a.addEventListener("change", (e) => {
	const t = e.target;
	const data = {
		type: t.getAttribute("class"),
		idx: parseInt(t.getAttribute("idx")),
		value: t.value
	}
	console.log(data);

	const xhr = new XMLHttpRequest();
	xhr.open('POST', '/update', true);
	xhr.setRequestHeader('Content-Type', 'application/json');
	xhr.send(JSON.stringify(data));
})})}, false);

