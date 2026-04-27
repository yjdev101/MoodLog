async function fetchWithAuth(url, options = {}) {
    options.headers = {
        ...options.headers,
        "Authorization": "Bearer " + localStorage.getItem("accessToken")
    };

    let res = await fetch(url, options);

    if (res.status === 401) {
        const refreshToken = localStorage.getItem("refreshToken");
        if (refreshToken) {
            const refreshRes = await fetch("/api/auth/refresh", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ refreshToken })
            });

            if (refreshRes.ok) {
                localStorage.setItem("accessToken", await refreshRes.text());
                options.headers["Authorization"] = "Bearer " + localStorage.getItem("accessToken");
                res = await fetch(url, options);
                return res;
            }
        }
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        location.href = "/login.html";
        throw new Error("인증 만료");
    }

    return res;
}
